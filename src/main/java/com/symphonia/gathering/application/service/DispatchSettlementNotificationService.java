package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.usecase.DispatchSettlementNotificationUseCase;
import com.symphonia.gathering.domain.entity.NotificationDelivery;
import com.symphonia.gathering.domain.entity.NotificationSchedule;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.policy.NotificationPolicy;
import com.symphonia.gathering.domain.repository.NotificationDeliveryRepository;
import com.symphonia.gathering.domain.repository.NotificationScheduleRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

// 예약 시각이 된 모임의 정산 메시지 발송을 시작한다. 실제 발송은 수신자별 발송 기록을 큐로 받은 컨슈머가 한다.
// 예약을 잠근 뒤 정산 건을 잠근다. 기록과 수정도 예약을 먼저 잠그므로 같은 순서로 잠가 교착 상태가 생기지 않는다.
// 기록이 예약을 잠그고 있으면 SKIP LOCKED로 건너뛰고 다음 주기에 다시 본다.
@CommandService
@RequiredArgsConstructor
public class DispatchSettlementNotificationService
        implements DispatchSettlementNotificationUseCase {
    private final NotificationScheduleRepository notificationScheduleRepository;
    private final SettlementRepository settlementRepository;
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
    private void notifySettlements(Long gatheringId, LocalDateTime now) {
        List<Settlement> settlements =
                settlementRepository.findAllUnnotifiedByGatheringIdForUpdate(gatheringId);
        settlements.forEach(
                settlement -> {
                    settlement.markNotified(now);
                    settlementRepository.save(settlement);
                });

        notificationDeliveryRepository.saveAll(
                NotificationDelivery.forSettlement(gatheringId, settlements, now));
    }
}
