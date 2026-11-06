package com.symphonia.notification.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.notification.application.dto.command.AnnounceCancellationCommand;
import com.symphonia.notification.application.dto.command.RescheduleNotificationCommand;
import com.symphonia.notification.application.dto.command.ScheduleReminderCommand;
import com.symphonia.notification.application.dto.result.NotificationScheduleResult;
import com.symphonia.notification.application.usecase.AnnounceCancellationUseCase;
import com.symphonia.notification.application.usecase.RescheduleNotificationUseCase;
import com.symphonia.notification.application.usecase.ScheduleReminderUseCase;
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

// 정산 건이 바뀔 때 발송 예약, 취소 알림, 리마인드를 바꾼다. gathering이 정산 건을 바꾸는 트랜잭션 안에서 부르므로 함께 커밋된다.
@CommandService
@RequiredArgsConstructor
public class NotificationCommandService
        implements RescheduleNotificationUseCase,
                AnnounceCancellationUseCase,
                ScheduleReminderUseCase {
    private final NotificationScheduleRepository notificationScheduleRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final Clock clock;

    // 두 결제자가 동시에 기록하거나 발송과 기록이 겹쳐도 같은 예약 행을 차례로 잠그므로, 마지막 기록 시각이 예약에 남는다.
    // 발송도 예약을 잠근 뒤 정산 건을 잠그므로, 부른 쪽이 정산 건을 바꾸기 전에 이 메서드를 불러야 교착 상태가 생기지 않는다.
    // 발송 뒤에 기록한 정산 건도 같은 방식으로 새 예약을 잡는다. 발송하면 예약 시각을 비우기 때문이다.
    @Override
    public NotificationScheduleResult reschedule(RescheduleNotificationCommand command) {
        NotificationSchedule notificationSchedule =
                notificationScheduleRepository.findOrCreateForUpdate(command.gatheringId());
        notificationSchedule.reschedule(command.recordedAt());

        return NotificationScheduleResult.from(
                notificationScheduleRepository.save(notificationSchedule));
    }

    // 정산 건의 발송 기록과 모임의 리마인드를 모두 잠그고 고르므로, 컨슈머가 내용을 만들거나 보내는 중이면 끝날 때까지 기다린다.
    // 잠그지 않고 보낸 기록만 고르면, 취소 직전에 내용을 만든 참여자가 취소된 금액의 메시지만 받고 취소 알림은 받지 못한다.
    @Override
    public void announceCancellation(AnnounceCancellationCommand command) {
        notificationDeliveryRepository.saveAll(
                NotificationDelivery.forCancellation(
                        command.gatheringId(),
                        command.settlementId(),
                        findReachedRecipientIds(command.gatheringId(), command.settlementId()),
                        LocalDateTime.now(clock)));
    }

    // 발송을 시작할 때도 리마인드를 만들므로, 모임의 예약을 먼저 잠가 같은 참여자의 리마인드를 동시에 만들지 않는다.
    // 발송도 예약을 잠근 뒤 정산 건을 잠그므로, 부른 쪽이 정산 건을 저장하기 전에 이 메서드를 불러야 교착 상태가 생기지 않는다.
    @Override
    public void scheduleReminder(ScheduleReminderCommand command) {
        notificationScheduleRepository.findOrCreateForUpdate(command.gatheringId());

        notificationDeliveryRepository.saveAll(
                NotificationDelivery.forReminders(
                        command.gatheringId(),
                        command.participantId(),
                        List.of(command.settlementId()),
                        List.of(NotificationPolicy.revertReminderTime(LocalDateTime.now(clock))),
                        notificationDeliveryRepository.findAllRemindersByGatheringId(
                                command.gatheringId())));
    }

    // 정산 메시지와 리마인드를 모두 받은 참여자에게도 취소 알림은 한 번만 보낸다.
    private List<Long> findReachedRecipientIds(Long gatheringId, Long settlementId) {
        return Stream.concat(
                        notificationDeliveryRepository
                                .findAllSettlementDeliveriesForUpdate(settlementId)
                                .stream(),
                        notificationDeliveryRepository
                                .findAllRemindersByGatheringIdForUpdate(gatheringId)
                                .stream()
                                .filter(reminder -> reminder.includes(settlementId)))
                .filter(NotificationDelivery::hasReachedRecipient)
                .map(NotificationDelivery::getParticipantId)
                .distinct()
                .toList();
    }
}
