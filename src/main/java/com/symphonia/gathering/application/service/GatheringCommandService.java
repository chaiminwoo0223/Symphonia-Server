package com.symphonia.gathering.application.service;

import com.symphonia.catalog.application.usecase.VerifyCatalogReferencesUseCase;
import com.symphonia.common.annotation.CommandService;
import com.symphonia.common.exception.BusinessException;
import com.symphonia.gathering.application.dto.command.ChooseGatheringPairingCommand;
import com.symphonia.gathering.application.dto.command.DeleteGatheringCommand;
import com.symphonia.gathering.application.dto.command.ExtendRecordingCommand;
import com.symphonia.gathering.application.dto.command.GatheringPairingCommand;
import com.symphonia.gathering.application.dto.command.OpenGatheringCommand;
import com.symphonia.gathering.application.dto.command.ReissueJoinTokenCommand;
import com.symphonia.gathering.application.dto.command.RenameGatheringCommand;
import com.symphonia.gathering.application.dto.result.GatheringJoinTokenResult;
import com.symphonia.gathering.application.dto.result.RecordingExtensionResult;
import com.symphonia.gathering.application.event.GatheringOpenedEvent;
import com.symphonia.gathering.application.usecase.ChooseGatheringPairingUseCase;
import com.symphonia.gathering.application.usecase.DeleteGatheringUseCase;
import com.symphonia.gathering.application.usecase.ExtendRecordingUseCase;
import com.symphonia.gathering.application.usecase.OpenGatheringUseCase;
import com.symphonia.gathering.application.usecase.PurgeGatheringUseCase;
import com.symphonia.gathering.application.usecase.ReissueJoinTokenUseCase;
import com.symphonia.gathering.application.usecase.RenameGatheringUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.GatheringOpenConflictException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringPairingRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@CommandService
@RequiredArgsConstructor
public class GatheringCommandService
        implements OpenGatheringUseCase,
                ReissueJoinTokenUseCase,
                ChooseGatheringPairingUseCase,
                ExtendRecordingUseCase,
                RenameGatheringUseCase,
                DeleteGatheringUseCase {
    private final GatheringRepository gatheringRepository;
    private final GatheringJoinTokenRepository gatheringJoinTokenRepository;
    private final GatheringRetentionRepository gatheringRetentionRepository;
    private final GatheringPairingRepository gatheringPairingRepository;
    private final GatheringTokenProvider gatheringTokenProvider;
    private final VerifyCatalogReferencesUseCase verifyCatalogReferencesUseCase;
    private final PurgeGatheringUseCase purgeGatheringUseCase;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public GatheringJoinTokenResult open(OpenGatheringCommand command) {
        Gathering gathering =
                gatheringRepository
                        .findByHostMemberIdAndClientRequestId(
                                command.memberId(), command.clientRequestId())
                        .orElseGet(() -> openNew(command));

        return issueJoinToken(gathering);
    }

    // 파기 시각이 지난 모임은 물리 삭제 전이라도 찾지 못한 것으로 본다. 삭제 뒤의 응답과 같게 하기 위해서다.
    @Override
    public GatheringJoinTokenResult reissue(ReissueJoinTokenCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        validateRetained(command.gatheringId(), now, GatheringNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findById(command.gatheringId())
                        .orElseThrow(GatheringNotFoundException::new);
        GatheringPolicy.validateHost(gathering.getHostMemberId(), command.memberId());
        GatheringPolicy.validateJoinable(gathering.getRecordingClosesAt(), now);

        return issueJoinToken(gathering);
    }

    // 이름은 기록 기간과 관계없이 고칠 수 있다. 모임을 저장하는 다른 요청이 옛 이름으로 덮어쓰지 않도록 전용 쿼리로만 바꾼다.
    @Override
    public void rename(RenameGatheringCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        validateRetained(command.gatheringId(), now, GatheringNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findById(command.gatheringId())
                        .orElseThrow(GatheringNotFoundException::new);
        GatheringPolicy.validateHost(gathering.getHostMemberId(), command.memberId());

        gatheringRepository.updateName(gathering.getId(), command.name());
    }

    // 데이터 키를 지우는 것과 나머지 행을 지우는 것을 한 트랜잭션에 묶는다. 자동 파기와 달리 둘을 따로 커밋하지 않는다.
    // 중간에 실패하면 모두 되돌아가 모임이 그대로 남고, 모임장이 다시 요청하면 된다.
    // 이미 지운 모임은 두 호출 모두 지울 행이 없어 성공으로 끝난다.
    // 따로 커밋하면 2단계가 실패했을 때 파기 스윕이 연 지 9일이 지난 모임만 읽어서 행이 한동안 남는다.
    @Override
    public void delete(DeleteGatheringCommand command) {
        purgeGatheringUseCase.discardDataKeyOfHostedGathering(
                command.memberId(), command.gatheringId());
        purgeGatheringUseCase.purgeWithoutSelectionSignal(command.gatheringId());
    }

    // 연장은 한 번만 할 수 있다. 이미 연장한 모임에 다시 보낸 요청은 응답만 사라졌거나 버튼을 두 번 누른 것이므로, 바꾸지 않고 성공으로 돌려준다.
    // 연장 여부는 모임을 저장하는 다른 요청이 옛 값으로 덮어쓰지 않도록 전용 쿼리로만 바꾼다.
    @Override
    public RecordingExtensionResult extend(ExtendRecordingCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        validateRetained(command.gatheringId(), now, GatheringNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findById(command.gatheringId())
                        .orElseThrow(GatheringNotFoundException::new);
        GatheringPolicy.validateHost(gathering.getHostMemberId(), command.memberId());

        if (!gathering.isRecordingExtended()) {
            GatheringPolicy.validateRecordingExtendable(gathering.getRecordingClosesAt(), now);
            gathering.extendRecording();
            gatheringRepository.markRecordingExtended(gathering.getId());
        }

        return RecordingExtensionResult.from(gathering);
    }

    // 실제로 먹은 조합이 추천과 달랐으면 모임장이 바로잡는다. 이미 고른 페어링이 있으면 덮어쓴다.
    @Override
    public void choose(ChooseGatheringPairingCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        validateRetained(command.gatheringId(), now, GatheringNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findById(command.gatheringId())
                        .orElseThrow(GatheringNotFoundException::new);
        GatheringPolicy.validateHost(gathering.getHostMemberId(), command.memberId());
        GatheringPolicy.validatePairingChangeable(gathering.getRecordingClosesAt(), now);

        savePairing(gathering.getId(), command.pairing());
    }

    // 요청 id가 같은 모임을 동시에 넣으면 늦은 요청은 넣지 못하고 409로 거절한다.
    // 늦은 요청이 참여 토큰을 새로 발급하면 먼저 응답받는 요청의 토큰이 무효가 되기 때문이다. 클라이언트가 다시 보내면 처음 연 모임을 돌려받는다.
    // 모임 페어링은 새로 연 요청만 저장한다.
    private Gathering openNew(OpenGatheringCommand command) {
        Gathering gathering =
                Gathering.open(
                        command.memberId(),
                        command.clientRequestId(),
                        command.name(),
                        LocalDateTime.now(clock));
        Gathering savedGathering =
                gatheringRepository
                        .saveIfAbsent(gathering)
                        .orElseThrow(GatheringOpenConflictException::new);
        command.findPairing().ifPresent(pairing -> savePairing(savedGathering.getId(), pairing));

        eventPublisher.publishEvent(new GatheringOpenedEvent());

        return savedGathering;
    }

    // 참여자가 피드백을 낼 때 없는 대상을 가리켜 실패하지 않도록, 고를 때 카탈로그에 있는지 확인한다.
    private void savePairing(Long gatheringId, GatheringPairingCommand pairing) {
        verifyCatalogReferencesUseCase.verify(pairing.toCatalogReferencesQuery());
        gatheringPairingRepository.save(gatheringId, pairing.toGatheringPairing());
    }

    // 모임마다 참여 토큰은 하나라서, 새로 발급하면 기존 토큰은 무효가 된다.
    private GatheringJoinTokenResult issueJoinToken(Gathering gathering) {
        String joinToken = gatheringTokenProvider.generate();
        gatheringJoinTokenRepository.save(joinToken, gathering.getId());

        return GatheringJoinTokenResult.of(
                gathering.getId(), joinToken, gathering.getRecordingClosesAt());
    }

    private void validateRetained(
            Long gatheringId, LocalDateTime now, Supplier<? extends BusinessException> notFound) {
        gatheringRetentionRepository
                .findByGatheringId(gatheringId)
                .filter(retention -> !retention.isExpired(now))
                .orElseThrow(notFound);
    }
}
