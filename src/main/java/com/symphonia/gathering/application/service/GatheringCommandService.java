package com.symphonia.gathering.application.service;

import com.symphonia.catalog.application.usecase.VerifyCatalogReferencesUseCase;
import com.symphonia.common.annotation.CommandService;
import com.symphonia.common.exception.BusinessException;
import com.symphonia.gathering.application.dto.command.ChooseGatheringPairingCommand;
import com.symphonia.gathering.application.dto.command.GatheringPairingCommand;
import com.symphonia.gathering.application.dto.command.JoinGatheringCommand;
import com.symphonia.gathering.application.dto.command.OpenGatheringCommand;
import com.symphonia.gathering.application.dto.command.ReissueJoinTokenCommand;
import com.symphonia.gathering.application.dto.result.GatheringJoinTokenResult;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.application.usecase.ChooseGatheringPairingUseCase;
import com.symphonia.gathering.application.usecase.JoinGatheringUseCase;
import com.symphonia.gathering.application.usecase.OpenGatheringUseCase;
import com.symphonia.gathering.application.usecase.ReissueJoinTokenUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
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
                ChooseGatheringPairingUseCase {
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
        Gathering gathering =
                Gathering.open(command.memberId(), command.name(), LocalDateTime.now(clock));
        Gathering savedGathering = gatheringRepository.save(gathering);
        command.findPairing().ifPresent(pairing -> savePairing(savedGathering.getId(), pairing));

        return issueJoinToken(savedGathering.getId());
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
        GatheringPolicy.validateJoinable(gathering.getOpenedAt(), now);

        return issueJoinToken(gathering.getId());
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
                        .findById(gatheringId)
                        .orElseThrow(GatheringNotFoundException::new);
        PhoneNumber phoneNumber = PhoneNumber.from(command.phoneNumber());
        GatheringPolicy.validateJoinable(gathering.getOpenedAt(), now);
        GatheringPolicy.validateConsent(command.consentAgreed(), command.consentVersion());
        GatheringPolicy.validateNotJoined(gathering.hasJoined(phoneNumber));

        gathering.join(command.name(), phoneNumber, command.consentVersion(), now);
        Gathering savedGathering = gatheringRepository.save(gathering);
        Participant participant = savedGathering.getParticipant(phoneNumber);
        String linkToken = gatheringTokenProvider.generate();
        participantLinkTokenRepository.save(linkToken, participant.getId());

        return ParticipantResult.of(participant, linkToken);
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
        GatheringPolicy.validatePairingChangeable(gathering.getOpenedAt(), now);

        savePairing(gathering.getId(), command.pairing());
    }

    // 참여자가 피드백을 낼 때 없는 대상을 가리켜 실패하지 않도록, 고를 때 카탈로그에 있는지 확인한다.
    private void savePairing(Long gatheringId, GatheringPairingCommand pairing) {
        verifyCatalogReferencesUseCase.verify(pairing.toCatalogReferencesQuery());
        gatheringPairingRepository.save(gatheringId, pairing.toGatheringPairing());
    }

    // 모임마다 참여 토큰은 하나라서, 새로 발급하면 기존 토큰은 무효가 된다.
    private GatheringJoinTokenResult issueJoinToken(Long gatheringId) {
        String joinToken = gatheringTokenProvider.generate();
        gatheringJoinTokenRepository.save(joinToken, gatheringId);

        return GatheringJoinTokenResult.of(gatheringId, joinToken);
    }

    private void validateRetained(
            Long gatheringId, LocalDateTime now, Supplier<? extends BusinessException> notFound) {
        gatheringRetentionRepository
                .findByGatheringId(gatheringId)
                .filter(retention -> !retention.isExpired(now))
                .orElseThrow(notFound);
    }
}
