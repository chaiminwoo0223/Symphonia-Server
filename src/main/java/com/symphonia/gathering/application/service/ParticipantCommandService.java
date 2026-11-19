package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.dto.command.ReissueParticipantLinkTokenCommand;
import com.symphonia.gathering.application.dto.command.RemoveParticipantCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.application.usecase.ReissueParticipantLinkTokenUseCase;
import com.symphonia.gathering.application.usecase.RemoveParticipantUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

// 모임장이 참여자를 정리하거나 링크를 잃은 참여자를 돕는 흐름이다.
// 링크를 잃은 참여자도 파기 전까지 분담금을 보내야 하므로 기록 기간 제한을 두지 않는다.
@CommandService
@RequiredArgsConstructor
public class ParticipantCommandService
        implements RemoveParticipantUseCase, ReissueParticipantLinkTokenUseCase {
    private final GatheringRepository gatheringRepository;
    private final GatheringRetentionRepository gatheringRetentionRepository;
    private final ParticipantLinkTokenRepository participantLinkTokenRepository;
    private final ParticipantBankAccountRepository participantBankAccountRepository;
    private final SettlementRepository settlementRepository;
    private final GatheringTokenProvider gatheringTokenProvider;
    private final Clock clock;

    // 개인 링크 토큰은 참여자마다 여러 개를 둘 수 있으므로, 새로 발급하기 전에 이전 토큰을 모두 지워 이전 링크를 무효로 만든다.
    @Override
    public ParticipantResult reissue(ReissueParticipantLinkTokenCommand command) {
        Gathering gathering = findManagedGathering(command.gatheringId(), command.memberId());
        GatheringPolicy.validateParticipantExists(
                gathering.hasParticipant(command.participantId()));

        Participant participant = gathering.getParticipant(command.participantId());
        String linkToken = gatheringTokenProvider.generate();
        participantLinkTokenRepository.deleteAllByParticipantId(participant.getId());
        participantLinkTokenRepository.save(linkToken, participant.getId());

        return ParticipantResult.of(participant, linkToken);
    }

    // 피드백 제출 표시는 분담금을 보내거나 받은 참여자만 남기므로, 정산 건 인원이 아닌 참여자에게는 없다.
    @Override
    public void remove(RemoveParticipantCommand command) {
        Gathering gathering = findManagedGathering(command.gatheringId(), command.memberId());
        GatheringPolicy.validateParticipantExists(
                gathering.hasParticipant(command.participantId()));
        GatheringPolicy.validateParticipantRemovable(
                settlementRepository.existsByParticipantId(command.participantId()));

        participantLinkTokenRepository.deleteAllByParticipantId(command.participantId());
        participantBankAccountRepository.deleteByParticipantId(command.participantId());
        gatheringRepository.deleteParticipant(command.participantId());
    }

    // 파기 시각이 지난 모임은 물리 삭제 전이라도 찾지 못한 것으로 본다. 삭제 뒤의 응답과 같게 하기 위해서다.
    private Gathering findManagedGathering(Long gatheringId, Long memberId) {
        LocalDateTime now = LocalDateTime.now(clock);
        gatheringRetentionRepository
                .findByGatheringId(gatheringId)
                .filter(retention -> !retention.isExpired(now))
                .orElseThrow(GatheringNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findById(gatheringId)
                        .orElseThrow(GatheringNotFoundException::new);
        GatheringPolicy.validateHost(gathering.getHostMemberId(), memberId);

        return gathering;
    }
}
