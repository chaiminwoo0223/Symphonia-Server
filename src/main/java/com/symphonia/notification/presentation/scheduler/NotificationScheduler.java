package com.symphonia.notification.presentation.scheduler;

import com.symphonia.notification.application.usecase.DispatchSettlementNotificationUseCase;
import com.symphonia.notification.application.usecase.GetDueNotificationSchedulesUseCase;
import com.symphonia.notification.application.usecase.PublishNotificationDeliveriesUseCase;
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
        name = "notification.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class NotificationScheduler {
    // 한 번의 스케줄 실행에서 이어서 발행하는 최대 횟수다. 100건씩 발행하므로 한 실행에 최대 1,000건이다.
    // 5분이 지나 실패로 세는 발송 기록은 발행 중인 상한에 들지 않아 이어서 발행이 길어질 수 있고, 스케줄러 스레드는 하나뿐이라 예약 처리를 막는다.
    private static final int MAX_PUBLISH_ROUNDS = 10;

    private final GetDueNotificationSchedulesUseCase getDueNotificationSchedulesUseCase;
    private final DispatchSettlementNotificationUseCase dispatchSettlementNotificationUseCase;
    private final PublishNotificationDeliveriesUseCase publishNotificationDeliveriesUseCase;

    // 모임마다 트랜잭션을 나눠, 한 모임에서 실패해도 다른 모임의 메시지는 나간다. 실패한 모임은 다음 주기에 다시 시도한다.
    @Scheduled(fixedDelayString = "${notification.poll-interval}")
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

    // 상한만큼 발행했으면 다음 주기를 기다리지 않고 이어서 발행하되, 한 실행에서 이어서 발행하는 횟수에 상한을 둔다.
    // 발행하지 않았거나 발행 중인 상한에 닿으면 UseCase가 false를 돌려주므로 바로 끝낸다. 횟수 상한에 닿은 나머지는 다음 주기에 처리한다.
    @Scheduled(fixedDelayString = "${notification.poll-interval}")
    public void publishPendingDeliveries() {
        for (int round = 0; round < MAX_PUBLISH_ROUNDS; round++) {
            if (!publishNotificationDeliveriesUseCase.publishPending()) {
                return;
            }
        }
    }
}
