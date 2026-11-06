package com.symphonia.notification.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.notification.application.dto.command.NotifySettlementsCommand;
import com.symphonia.notification.application.dto.result.NotificationTargetResult;
import com.symphonia.notification.application.provider.NotificationTargetProvider;
import com.symphonia.notification.application.usecase.DispatchSettlementNotificationUseCase;
import com.symphonia.notification.domain.entity.NotificationDelivery;
import com.symphonia.notification.domain.entity.NotificationSchedule;
import com.symphonia.notification.domain.policy.NotificationPolicy;
import com.symphonia.notification.domain.repository.NotificationDeliveryRepository;
import com.symphonia.notification.domain.repository.NotificationScheduleRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

// 예약 시각이 된 모임의 정산 메시지 발송을 시작한다. 실제 발송은 수신자별 발송 기록을 큐로 받은 컨슈머가 한다.
// 예약을 잠근 뒤 NotificationTargetProvider가 정산 건을 잠근다. 기록과 수정도 예약을 먼저 잠그므로 같은 순서로 잠가 교착 상태가 생기지 않는다.
// 기록이 예약을 잠그고 있으면 SKIP LOCKED로 건너뛰고 다음 주기에 다시 본다.
@CommandService
@RequiredArgsConstructor
public class DispatchSettlementNotificationService
        implements DispatchSettlementNotificationUseCase {
    private final NotificationScheduleRepository notificationScheduleRepository;
    private final NotificationTargetProvider notificationTargetProvider;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final Clock clock;

    // 다른 서버가 이미 발송을 시작했거나 그사이 기록으로 예약이 늦춰졌으면 아무것도 하지 않는다.
    @Override
    public void dispatch(Long gatheringId) {
        LocalDateTime now = LocalDateTime.now(clock);
        notificationScheduleRepository
                .findDueForUpdate(gatheringId, now)
                .ifPresent(notificationSchedule -> dispatchOrDefer(notificationSchedule, now));
    }

    // 보낼 수 없는 시간대에 예약 시각이 되면 보내지 않고 그다음 10:00로 미룬다.
    private void dispatchOrDefer(NotificationSchedule notificationSchedule, LocalDateTime now) {
        if (NotificationPolicy.isSendable(now)) {
            notifySettlements(notificationSchedule.getGatheringId(), now);
            notificationSchedule.clear();
        } else {
            notificationSchedule.defer(now);
        }
        notificationScheduleRepository.save(notificationSchedule);
    }

    // 예약 뒤에 모두 취소됐으면 보낼 정산 건이 없어 발송 기록도 만들지 않는다.
    // 정산 메시지와 함께 그 수신자의 3일 뒤, 5일 뒤 리마인드를 만든다. 그때까지 모두 내면 리마인드는 보내지 않고 건너뛴다.
    private void notifySettlements(Long gatheringId, LocalDateTime now) {
        List<NotificationTargetResult> targets =
                notificationTargetProvider.notifySettlements(
                        NotifySettlementsCommand.of(gatheringId, now));
        List<NotificationDelivery> reminders =
                notificationDeliveryRepository.findAllRemindersByGatheringId(gatheringId);

        notificationDeliveryRepository.saveAll(
                targets.stream()
                        .flatMap(target -> deliveriesOf(gatheringId, target, reminders, now))
                        .toList());
    }

    private Stream<NotificationDelivery> deliveriesOf(
            Long gatheringId,
            NotificationTargetResult target,
            List<NotificationDelivery> reminders,
            LocalDateTime now) {
        return Stream.concat(
                Stream.of(
                        NotificationDelivery.forSettlement(
                                gatheringId, target.participantId(), target.settlementIds(), now)),
                NotificationDelivery.forReminders(
                        gatheringId,
                        target.participantId(),
                        target.settlementIds(),
                        NotificationPolicy.reminderTimes(now),
                        reminders)
                        .stream());
    }
}
