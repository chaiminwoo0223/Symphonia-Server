package com.symphonia.gathering.application.service;

import com.symphonia.catalog.application.usecase.VerifyCatalogReferencesUseCase;
import com.symphonia.common.annotation.CommandService;
import com.symphonia.common.exception.BusinessException;
import com.symphonia.gathering.application.dto.command.ChooseGatheringPairingCommand;
import com.symphonia.gathering.application.dto.command.ExtendRecordingCommand;
import com.symphonia.gathering.application.dto.command.GatheringPairingCommand;
import com.symphonia.gathering.application.dto.command.JoinGatheringAsHostCommand;
import com.symphonia.gathering.application.dto.command.JoinGatheringCommand;
import com.symphonia.gathering.application.dto.command.OpenGatheringCommand;
import com.symphonia.gathering.application.dto.command.ReissueJoinTokenCommand;
import com.symphonia.gathering.application.dto.command.RenameGatheringCommand;
import com.symphonia.gathering.application.dto.result.GatheringJoinTokenResult;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.application.dto.result.RecordingExtensionResult;
import com.symphonia.gathering.application.usecase.ChooseGatheringPairingUseCase;
import com.symphonia.gathering.application.usecase.ExtendRecordingUseCase;
import com.symphonia.gathering.application.usecase.JoinGatheringAsHostUseCase;
import com.symphonia.gathering.application.usecase.JoinGatheringUseCase;
import com.symphonia.gathering.application.usecase.OpenGatheringUseCase;
import com.symphonia.gathering.application.usecase.ReissueJoinTokenUseCase;
import com.symphonia.gathering.application.usecase.RenameGatheringUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.GatheringOpenConflictException;
import com.symphonia.gathering.domain.exception.JoinTokenNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringPairingRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class GatheringCommandService
        implements OpenGatheringUseCase,
                ReissueJoinTokenUseCase,
                JoinGatheringUseCase,
                JoinGatheringAsHostUseCase,
                ChooseGatheringPairingUseCase,
                ExtendRecordingUseCase,
                RenameGatheringUseCase {
    private final GatheringRepository gatheringRepository;
    private final GatheringJoinTokenRepository gatheringJoinTokenRepository;
    private final ParticipantLinkTokenRepository participantLinkTokenRepository;
    private final GatheringRetentionRepository gatheringRetentionRepository;
    private final GatheringPairingRepository gatheringPairingRepository;
    private final GatheringTokenProvider gatheringTokenProvider;
    private final VerifyCatalogReferencesUseCase verifyCatalogReferencesUseCase;
    private final Clock clock;

    @Override
    public GatheringJoinTokenResult open(OpenGatheringCommand command) {
        // 같은 요청 id로 다시 온 요청은 내용이 달라도 처음 연 모임을 돌려준다. 참여 토큰은 해시만 저장하므로 새로 발급하고 기존 토큰은 무효가 된다.
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

    @Override
    public ParticipantResult join(JoinGatheringCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        Long gatheringId =
                gatheringJoinTokenRepository
                        .findGatheringIdByValue(command.joinToken())
                        .orElseThrow(JoinTokenNotFoundException::new);
        validateRetained(gatheringId, now, JoinTokenNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findByIdForUpdate(gatheringId)
                        .orElseThrow(GatheringNotFoundException::new);

        return joinParticipant(
                gathering,
                command.name(),
                command.phoneNumber(),
                command.consentAgreed(),
                command.consentVersion(),
                now);
    }

    // 모임장도 참여 QR 없이 로그인한 채로 참여자가 될 수 있다. 참여 기간, 동의, 휴대폰 중복, 인원 상한은 QR 참여와 같은 규칙을 따른다.
    // 참여자가 되면 정산 건 인원에 들어갈 수 있고, 그 정산 건의 결제자에게 이름과 휴대폰이 전달되므로 동의를 똑같이 받는다.
    @Override
    public ParticipantResult joinAsHost(JoinGatheringAsHostCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        validateRetained(command.gatheringId(), now, GatheringNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findByIdForUpdate(command.gatheringId())
                        .orElseThrow(GatheringNotFoundException::new);
        GatheringPolicy.validateHost(gathering.getHostMemberId(), command.memberId());

        return joinParticipant(
                gathering,
                command.name(),
                command.phoneNumber(),
                command.consentAgreed(),
                command.consentVersion(),
                now);
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

    // 호출하는 쪽이 모임 행을 잠근 채로 넘겨야 인원 확인과 저장 사이에 다른 참여가 끼어들지 않는다.
    private ParticipantResult joinParticipant(
            Gathering gathering,
            String name,
            String rawPhoneNumber,
            boolean consentAgreed,
            String consentVersion,
            LocalDateTime now) {
        PhoneNumber phoneNumber = PhoneNumber.from(rawPhoneNumber);
        GatheringPolicy.validateJoinable(gathering.getRecordingClosesAt(), now);
        GatheringPolicy.validateConsent(consentAgreed, consentVersion);
        GatheringPolicy.validateNotJoined(gathering.hasJoined(phoneNumber));
        GatheringPolicy.validateParticipantAddable(gathering.getParticipants().size());

        gathering.join(name, phoneNumber, consentVersion, now);
        Gathering savedGathering = gatheringRepository.save(gathering);
        Participant participant = savedGathering.getParticipant(phoneNumber);
        String linkToken = gatheringTokenProvider.generate();
        participantLinkTokenRepository.save(linkToken, participant.getId());

        return ParticipantResult.of(participant, linkToken);
    }
}
