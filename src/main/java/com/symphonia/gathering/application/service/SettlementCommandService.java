package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.dto.command.CancelSettlementCommand;
import com.symphonia.gathering.application.dto.command.RecordSettlementCommand;
import com.symphonia.gathering.application.dto.command.UpdateSettlementCommand;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.event.SettlementRecordedEvent;
import com.symphonia.gathering.application.usecase.CancelSettlementUseCase;
import com.symphonia.gathering.application.usecase.RecordSettlementUseCase;
import com.symphonia.gathering.application.usecase.UpdateSettlementUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.SettlementNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.SplitMethod;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

// 정산 건은 모임을 만든 회원이 JWT로 기록하고 수정하고 취소한다. 결제자는 기록할 때 모임장이 지정한다.
// 결제자가 직접 기록하면 모임장이 없던 자리의 정산 건에 모임장이 인원으로 들어갈 수 있기 때문이다.
@CommandService
@RequiredArgsConstructor
public class SettlementCommandService
        implements RecordSettlementUseCase, UpdateSettlementUseCase, CancelSettlementUseCase {
    private final GatheringRepository gatheringRepository;
    private final GatheringRetentionRepository gatheringRetentionRepository;
    private final SettlementRepository settlementRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public SettlementResult record(RecordSettlementCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        Gathering gathering = findManagedGathering(command.gatheringId(), command.memberId(), now);
        GatheringPolicy.validateParticipantExists(
                gathering.hasParticipant(command.payerParticipantId()));

        return settlementRepository
                .findByPayerParticipantIdAndClientRequestId(
                        command.payerParticipantId(), command.clientRequestId())
                .map(recordedSettlement -> toResult(recordedSettlement, gathering))
                .orElseGet(() -> recordNew(command, gathering, now));
    }

    @Override
    public SettlementResult update(UpdateSettlementCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        Gathering gathering = findManagedGathering(command.gatheringId(), command.memberId(), now);
        Settlement settlement = findSettlement(command.settlementId(), gathering.getId());

        SettlementPolicy.validateRecordable(gathering.getRecordingClosesAt(), now);
        SettlementPolicy.validateGatheringRound(gathering.hasRound(command.roundId()));
        SettlementPolicy.validateGatheringParticipants(
                gathering.hasParticipants(command.participantIds()));
        SplitMethod splitMethod =
                SplitMethod.of(
                        command.totalAmount(),
                        settlement.getPayerParticipantId(),
                        command.participantIds(),
                        command.fixedAmounts());

        settlement.update(command.roundId(), splitMethod, now);
        Settlement savedSettlement = settlementRepository.save(settlement);

        return SettlementResult.of(savedSettlement, gathering.getRound(command.roundId()));
    }

    @Override
    public void cancel(CancelSettlementCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        Gathering gathering = findManagedGathering(command.gatheringId(), command.memberId(), now);
        Settlement settlement = findSettlement(command.settlementId(), gathering.getId());

        if (settlement.cancel(now)) {
            settlementRepository.save(settlement);
        }
    }

    // 파기 시각이 지난 모임은 물리 삭제 전이라도 찾지 못한 것으로 본다. 삭제 뒤의 응답과 같게 하기 위해서다.
    // 모임 행을 잠가 참여자의 철회와 모임장의 참여자 제거와 차례로 처리한다.
    // 잠그지 않으면 철회와 동시에 기록된 정산 건이 익명화한 참여자를 가리킬 수 있다.
    private Gathering findManagedGathering(Long gatheringId, Long memberId, LocalDateTime now) {
        gatheringRetentionRepository
                .findByGatheringId(gatheringId)
                .filter(retention -> !retention.isExpired(now))
                .orElseThrow(GatheringNotFoundException::new);

        Gathering gathering =
                gatheringRepository
                        .findByIdForUpdate(gatheringId)
                        .orElseThrow(GatheringNotFoundException::new);
        GatheringPolicy.validateHost(gathering.getHostMemberId(), memberId);

        return gathering;
    }

    private SettlementResult recordNew(
            RecordSettlementCommand command, Gathering gathering, LocalDateTime now) {
        SettlementPolicy.validateRecordable(gathering.getRecordingClosesAt(), now);
        SettlementPolicy.validateGatheringRound(gathering.hasRound(command.roundId()));
        SettlementPolicy.validateGatheringParticipants(
                gathering.hasParticipants(command.participantIds()));
        SplitMethod splitMethod =
                SplitMethod.of(
                        command.totalAmount(),
                        command.payerParticipantId(),
                        command.participantIds(),
                        command.fixedAmounts());

        Settlement settlement =
                Settlement.record(
                        gathering.getId(),
                        command.roundId(),
                        splitMethod,
                        command.clientRequestId(),
                        now);
        Optional<Settlement> savedSettlement = settlementRepository.saveIfAbsent(settlement);

        savedSettlement.ifPresent(
                saved -> eventPublisher.publishEvent(new SettlementRecordedEvent()));

        return toResult(
                savedSettlement.orElseGet(() -> findRecordedConcurrently(command)), gathering);
    }

    // 저장하지 못했다면 같은 요청 id의 정산 건이 이미 커밋된 것이므로 반드시 찾는다.
    private Settlement findRecordedConcurrently(RecordSettlementCommand command) {
        return settlementRepository
                .findByPayerParticipantIdAndClientRequestId(
                        command.payerParticipantId(), command.clientRequestId())
                .orElseThrow();
    }

    // 처음 읽은 모임 뒤에 다른 요청이 차수를 더하고 그 차수에 기록했다면 정산 건의 차수가 처음 읽은 모임에 없다. 그때는 모임을 다시 읽는다.
    private SettlementResult toResult(Settlement settlement, Gathering gathering) {
        Gathering currentGathering =
                gathering.hasRound(settlement.getRoundId())
                        ? gathering
                        : gatheringRepository
                                .findById(settlement.getGatheringId())
                                .orElseThrow(GatheringNotFoundException::new);

        return SettlementResult.of(settlement, currentGathering.getRound(settlement.getRoundId()));
    }

    // 다른 모임의 정산 건은 그 모임의 모임장이 아니면 존재를 알 수 없어야 하므로 찾지 못한 것으로 본다.
    private Settlement findSettlement(Long settlementId, Long gatheringId) {
        return settlementRepository
                .findById(settlementId)
                .filter(settlement -> settlement.getGatheringId().equals(gatheringId))
                .orElseThrow(SettlementNotFoundException::new);
    }
}
