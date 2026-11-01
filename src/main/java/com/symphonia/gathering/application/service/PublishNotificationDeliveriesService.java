package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.usecase.PublishNotificationDeliveriesUseCase;
import com.symphonia.gathering.domain.entity.NotificationDelivery;
import com.symphonia.gathering.domain.publisher.NotificationDeliveryPublisher;
import com.symphonia.gathering.domain.repository.NotificationDeliveryRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

// 보낼 시각이 된 발송 기록을 큐에 발행한다. 처음 발행, 발행 실패 뒤 재발행, 미룬 발송, 잃어버린 메시지의 재발행을 모두 맡는다.
// 같은 발송 기록이 두 번 발행돼도 컨슈머가 발송 기록을 잠그고 맡으므로 한 번만 보낸다.
@CommandService
@RequiredArgsConstructor
public class PublishNotificationDeliveriesService implements PublishNotificationDeliveriesUseCase {
    // 한 번에 발행할 발송 기록 수다. 남은 발송 기록은 다음 주기에 이어서 발행한다.
    private static final int PUBLISH_LIMIT = 100;

    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationDeliveryPublisher notificationDeliveryPublisher;
    private final Clock clock;

    // 브로커가 받았다고 확인한 발송 기록만 발행했다고 남긴다.
    // 브로커가 확인하지 않은 발송 기록은 다음 주기에 다시 발행한다.
    @Override
    public void publishPending() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> publishableIds =
                notificationDeliveryRepository.findPublishableIdsForUpdate(
                        now,
                        NotificationDelivery.republishThreshold(now),
                        NotificationDelivery.claimExpiryThreshold(now),
                        PUBLISH_LIMIT);

        List<Long> publishedIds = notificationDeliveryPublisher.publishAll(publishableIds);
        notificationDeliveryRepository.markPublished(publishedIds, now);
    }
}
