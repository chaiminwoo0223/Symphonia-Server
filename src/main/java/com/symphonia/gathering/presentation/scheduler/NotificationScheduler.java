package com.symphonia.gathering.presentation.scheduler;

import com.symphonia.gathering.application.usecase.DispatchSettlementNotificationUseCase;
import com.symphonia.gathering.application.usecase.GetDueNotificationSchedulesUseCase;
import com.symphonia.gathering.application.usecase.PublishNotificationDeliveriesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 예약 시각이 된 정산 메시지의 발송을 시작하고, 보낼 시각이 된 발송 기록을 큐에 발행한다.
// 여러 서버가 함께 돌아도 예약과 발송 기록을 잠그고 다른 서버가 잠근 행은 건너뛰므로 한 번만 처리한다.
// 테스트는 시각을 고정해 UseCase를 직접 부르므로, 테스트 프로파일에서는 이 스케줄러를 띄우지 않는다.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "gathering.notification.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class NotificationScheduler {
    private final GetDueNotificationSchedulesUseCase getDueNotificationSchedulesUseCase;
    private final DispatchSettlementNotificationUseCase dispatchSettlementNotificationUseCase;
    private final PublishNotificationDeliveriesUseCase publishNotificationDeliveriesUseCase;

    // 모임마다 트랜잭션을 나눠, 한 모임에서 실패해도 다른 모임의 메시지는 나간다. 실패한 모임은 다음 주기에 다시 시도한다.
    @Scheduled(fixedDelayString = "${gathering.notification.poll-interval}")
    public void dispatchDueSchedules() {
        getDueNotificationSchedulesUseCase
                .getDueGatheringIds()
                .forEach(
                        gatheringId -> {
                            try {
                                dispatchSettlementNotificationUseCase.dispatch(gatheringId);
                            } catch (RuntimeException e) {
                                log.error("[Notification] gatheringId={} 발송 시작 실패", gatheringId, e);
                            }
                        });
    }

    @Scheduled(fixedDelayString = "${gathering.notification.poll-interval}")
    public void publishPendingDeliveries() {
        publishNotificationDeliveriesUseCase.publishPending();
    }
}
