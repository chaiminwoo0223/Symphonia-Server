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
import com.symphonia.notification.application.dto.command.AnnounceCancellationCommand;
import com.symphonia.notification.application.dto.command.RescheduleNotificationCommand;
import com.symphonia.notification.application.dto.result.NotificationScheduleResult;
import com.symphonia.notification.application.usecase.AnnounceCancellationUseCase;
import com.symphonia.notification.application.usecase.RescheduleNotificationUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

// 정산 건은 결제자가 개인 링크 토큰으로 기록하고 수정하고 취소한다. 요청한 참여자가 곧 결제자다.
// 기록과 수정은 정산 건을 바꾸기 전에 notification의 발송 예약을 먼저 잠근다.
// 발송도 예약을 잠근 뒤 정산 건을 잠그므로, 같은 순서로 잠가 교착 상태가 생기지 않는다.
// notification의 UseCase 둘은 정산 건 변경이 발송에 미치는 영향이라는 한 관심사라 같은 Service가 부른다(#159, #168).
@CommandService
@RequiredArgsConstructor
public class SettlementCommandService
        implements RecordSettlementUseCase, UpdateSettlementUseCase, CancelSettlementUseCase {
    private final GetLinkedParticipantUseCase getLinkedParticipantUseCase;
    private final GatheringRepository gatheringRepository;
    private final ParticipantBankAccountRepository participantBankAccountRepository;
    private final SettlementRepository settlementRepository;
    private final RescheduleNotificationUseCase rescheduleNotificationUseCase;
    private final AnnounceCancellationUseCase announceCancellationUseCase;
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

        NotificationScheduleResult schedule =
                rescheduleNotificationUseCase.reschedule(
                        RescheduleNotificationCommand.of(gathering.getId(), now));

        Settlement settlement = Settlement.record(gathering.getId(), splitMethod, now);
        Settlement savedSettlement = settlementRepository.save(settlement);

        return SettlementResult.of(savedSettlement, schedule.scheduledAt());
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
        SettlementPolicy.validateGatheringParticipants(
                gathering.hasParticipants(command.participantIds()));
        SplitMethod splitMethod =
                SplitMethod.of(
                        command.totalAmount(),
                        participantId,
                        command.participantIds(),
                        command.fixedAmounts());

        NotificationScheduleResult schedule =
                rescheduleNotificationUseCase.reschedule(
                        RescheduleNotificationCommand.of(gathering.getId(), now));

        settlement.update(splitMethod, now);
        Settlement savedSettlement = settlementRepository.save(settlement);

        return SettlementResult.of(savedSettlement, schedule.scheduledAt());
    }

    @Override
    public void cancel(CancelSettlementCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Settlement settlement = findSettlement(command.settlementId());
        SettlementPolicy.validatePayer(settlement.getPayerParticipantId(), participantId);

        LocalDateTime now = LocalDateTime.now(clock);
        settlement.cancel(now);
        settlementRepository.save(settlement);

        Optional.of(settlement)
                .filter(Settlement::isNotified)
                .ifPresent(this::announceCancellation);
    }

    private Long findParticipantId(String linkToken) {
        return getLinkedParticipantUseCase.getLinkedParticipant(linkToken).participantId();
    }

    private Settlement findSettlement(Long settlementId) {
        return settlementRepository
                .findById(settlementId)
                .orElseThrow(SettlementNotFoundException::new);
    }

    private void announceCancellation(Settlement settlement) {
        announceCancellationUseCase.announceCancellation(
                AnnounceCancellationCommand.of(settlement.getGatheringId(), settlement.getId()));
    }
}
