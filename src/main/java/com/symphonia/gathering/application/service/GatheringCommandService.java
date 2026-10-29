package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.dto.command.JoinGatheringCommand;
import com.symphonia.gathering.application.dto.command.OpenGatheringCommand;
import com.symphonia.gathering.application.dto.command.ReissueJoinTokenCommand;
import com.symphonia.gathering.application.dto.result.GatheringJoinTokenResult;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.application.usecase.JoinGatheringUseCase;
import com.symphonia.gathering.application.usecase.OpenGatheringUseCase;
import com.symphonia.gathering.application.usecase.ReissueJoinTokenUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class GatheringCommandService
        implements OpenGatheringUseCase, ReissueJoinTokenUseCase, JoinGatheringUseCase {
    private final GatheringRepository gatheringRepository;
    private final GatheringJoinTokenRepository gatheringJoinTokenRepository;
    private final ParticipantLinkTokenRepository participantLinkTokenRepository;
    private final GatheringTokenProvider gatheringTokenProvider;

    @Override
    public GatheringJoinTokenResult open(OpenGatheringCommand command) {
        Gathering gathering = Gathering.open(command.memberId(), command.name());
        Gathering savedGathering = gatheringRepository.save(gathering);

        return issueJoinToken(savedGathering.getId());
    }

    @Override
    public GatheringJoinTokenResult reissue(ReissueJoinTokenCommand command) {
        Gathering gathering =
                gatheringRepository
                        .findById(command.gatheringId())
                        .orElseThrow(GatheringNotFoundException::new);
        GatheringPolicy.validateHost(gathering.getHostMemberId(), command.memberId());
        GatheringPolicy.validateJoinable(gathering.getCreatedAt(), LocalDateTime.now());

        return issueJoinToken(gathering.getId());
    }

    @Override
    public ParticipantResult join(JoinGatheringCommand command) {
        Long gatheringId =
                gatheringJoinTokenRepository
                        .findGatheringIdByValue(command.joinToken())
                        .orElseThrow(GatheringNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findById(gatheringId)
                        .orElseThrow(GatheringNotFoundException::new);
        PhoneNumber phoneNumber = PhoneNumber.from(command.phoneNumber());
        LocalDateTime now = LocalDateTime.now();
        GatheringPolicy.validateJoinable(gathering.getCreatedAt(), now);
        GatheringPolicy.validateConsent(command.consentAgreed(), command.consentVersion());
        GatheringPolicy.validateNotJoined(gathering.hasJoined(phoneNumber));

        gathering.join(
                command.name(), phoneNumber, command.nonDrinker(), command.consentVersion(), now);
        Gathering savedGathering = gatheringRepository.save(gathering);
        Participant participant = savedGathering.getParticipant(phoneNumber);
        String linkToken = gatheringTokenProvider.generate();
        participantLinkTokenRepository.save(linkToken, participant.getId());

        return ParticipantResult.of(participant, linkToken);
    }

    // 모임마다 참여 토큰은 하나라서, 새로 발급하면 기존 토큰은 무효가 된다.
    private GatheringJoinTokenResult issueJoinToken(Long gatheringId) {
        String joinToken = gatheringTokenProvider.generate();
        gatheringJoinTokenRepository.save(joinToken, gatheringId);

        return GatheringJoinTokenResult.of(gatheringId, joinToken);
    }
}
