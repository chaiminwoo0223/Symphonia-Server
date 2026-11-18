package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.dto.command.CancelSettlementCommand;
import com.symphonia.gathering.application.dto.command.RecordSettlementCommand;
import com.symphonia.gathering.application.dto.command.UpdateSettlementCommand;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.usecase.CancelSettlementUseCase;
import com.symphonia.gathering.application.usecase.GetLinkedParticipantUseCase;
import com.symphonia.gathering.application.usecase.RecordSettlementUseCase;
import com.symphonia.gathering.application.usecase.UpdateSettlementUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.SettlementNotFoundException;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.SplitMethod;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

// 정산 건은 결제자가 개인 링크 토큰으로 기록하고 수정하고 취소한다. 요청한 참여자가 곧 결제자다.
@CommandService
@RequiredArgsConstructor
public class SettlementCommandService
        implements RecordSettlementUseCase, UpdateSettlementUseCase, CancelSettlementUseCase {
    private final GetLinkedParticipantUseCase getLinkedParticipantUseCase;
    private final GatheringRepository gatheringRepository;
    private final ParticipantBankAccountRepository participantBankAccountRepository;
    private final SettlementRepository settlementRepository;
    private final Clock clock;

    @Override
    public SettlementResult record(RecordSettlementCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Gathering gathering =
                gatheringRepository
                        .findByParticipantId(participantId)
                        .orElseThrow(GatheringNotFoundException::new);
        LocalDateTime now = LocalDateTime.now(clock);
        SettlementPolicy.validateRecordable(gathering.getOpenedAt(), now);
        SettlementPolicy.validateGatheringRound(gathering.hasRound(command.roundId()));
        SettlementPolicy.validateGatheringParticipants(
                gathering.hasParticipants(command.participantIds()));
        SettlementPolicy.validateBankAccountRegistered(
                participantBankAccountRepository.existsByParticipantId(participantId));
        SplitMethod splitMethod =
                SplitMethod.of(
                        command.totalAmount(),
                        participantId,
                        command.participantIds(),
                        command.fixedAmounts());

        Settlement settlement =
                Settlement.record(
                        gathering.getId(),
                        command.roundId(),
                        splitMethod,
                        command.clientRequestId(),
                        now);
        // 같은 요청 id로 다시 온 요청이면 처음 기록한 정산 건이 돌아오므로, 차수도 그 정산 건의 차수를 쓴다.
        Settlement savedSettlement = settlementRepository.saveIfAbsent(settlement);

        return SettlementResult.of(
                savedSettlement, gathering.getRound(savedSettlement.getRoundId()));
    }

    @Override
    public SettlementResult update(UpdateSettlementCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Settlement settlement = findSettlement(command.settlementId());
        SettlementPolicy.validatePayer(settlement.getPayerParticipantId(), participantId);
        Gathering gathering =
                gatheringRepository
                        .findById(settlement.getGatheringId())
                        .orElseThrow(GatheringNotFoundException::new);
        LocalDateTime now = LocalDateTime.now(clock);
        SettlementPolicy.validateRecordable(gathering.getOpenedAt(), now);
        SettlementPolicy.validateGatheringRound(gathering.hasRound(command.roundId()));
        SettlementPolicy.validateGatheringParticipants(
                gathering.hasParticipants(command.participantIds()));
        SplitMethod splitMethod =
                SplitMethod.of(
                        command.totalAmount(),
                        participantId,
                        command.participantIds(),
                        command.fixedAmounts());

        settlement.update(command.roundId(), splitMethod, now);
        Settlement savedSettlement = settlementRepository.save(settlement);

        return SettlementResult.of(savedSettlement, gathering.getRound(command.roundId()));
    }

    @Override
    public void cancel(CancelSettlementCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Settlement settlement = findSettlement(command.settlementId());
        SettlementPolicy.validatePayer(settlement.getPayerParticipantId(), participantId);

        LocalDateTime now = LocalDateTime.now(clock);
        settlement.cancel(now);
        settlementRepository.save(settlement);
    }

    private Long findParticipantId(String linkToken) {
        return getLinkedParticipantUseCase.getLinkedParticipant(linkToken).participantId();
    }

    private Settlement findSettlement(Long settlementId) {
        return settlementRepository
                .findById(settlementId)
                .orElseThrow(SettlementNotFoundException::new);
    }
}
