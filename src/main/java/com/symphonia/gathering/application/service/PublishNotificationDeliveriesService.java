package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.usecase.PublishNotificationDeliveriesUseCase;
import com.symphonia.notification.domain.entity.NotificationDelivery;
import com.symphonia.notification.domain.publisher.NotificationDeliveryPublisher;
import com.symphonia.notification.domain.repository.NotificationDeliveryRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

// 보낼 시각이 된 발송 기록을 큐에 발행한다. 처음 발행, 발행 실패 뒤 재발행, 미룬 발송, 재시도를 기다린 발송을 모두 맡는다.
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
        // 재시도를 기다리게 하면 발행 시각이 비므로, 상태를 바꾸기 전에 나눈다.
        Map<Boolean, List<NotificationDelivery>> deliveriesByStalled =
                notificationDeliveryRepository
                        .findPublishableForUpdate(
                                now,
                                NotificationDelivery.republishThreshold(now),
                                NotificationDelivery.claimExpiryThreshold(now),
                                PUBLISH_LIMIT)
                        .stream()
                        .collect(Collectors.partitioningBy(NotificationDelivery::isPublished));

        retryStalledLater(deliveriesByStalled.get(true), now);

        List<Long> publishedIds =
                notificationDeliveryPublisher.publishAll(
                        deliveriesByStalled.get(false).stream()
                                .map(NotificationDelivery::getId)
                                .toList());
        notificationDeliveryRepository.markPublished(publishedIds, now);
    }

    // 발행했는데 보내지 못한 발송 기록은 원인과 관계없이 시도 한 번으로 세고, 외부 발송 실패와 같은 간격을 기다렸다가 다시 발행한다.
    // 컨슈머의 트랜잭션은 예외로 롤백되므로 시도 횟수를 이 트랜잭션에서 남겨야, 계속 실패하는 발송 기록도 FAILED로 끝난다.
    private void retryStalledLater(
            List<NotificationDelivery> stalledDeliveries, LocalDateTime now) {
        stalledDeliveries.forEach(notificationDelivery -> notificationDelivery.retryLater(now));
        notificationDeliveryRepository.saveAll(stalledDeliveries);
    }
}
