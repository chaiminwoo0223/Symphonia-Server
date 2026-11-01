package com.symphonia.notification.infrastructure.messaging;

import com.symphonia.notification.domain.publisher.NotificationDeliveryPublisher;
import com.symphonia.notification.infrastructure.messaging.config.properties.NotificationQueueProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

// 메시지를 모두 발행한 뒤 브로커의 확인(publisher confirm)을 한꺼번에 기다린다.
// 한 건씩 확인을 기다리면 발송 기록을 잠근 채 브로커 왕복을 건수만큼 기다리기 때문이다.
// 큐로 라우팅되지 않아 돌아온 메시지도 브로커는 받았다고 확인하므로, 돌아온 메시지인지 따로 확인해 발행하지 않은 것으로 본다.
// 메시지는 기본값대로 persistent로 발행해 브로커가 재시작돼도 남는다.
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitNotificationDeliveryPublisher implements NotificationDeliveryPublisher {
    private static final long CONFIRM_TIMEOUT_MILLIS = 5_000;

    private final RabbitTemplate rabbitTemplate;
    private final NotificationQueueProperties notificationQueueProperties;

    @Override
    public List<Long> publishAll(List<Long> deliveryIds) {
        List<CorrelationData> publishedMessages = publishUntilFailure(deliveryIds);
        long confirmDeadlineNanos =
                System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(CONFIRM_TIMEOUT_MILLIS);

        return publishedMessages.stream()
                .filter(publishedMessage -> isRoutedToQueue(publishedMessage, confirmDeadlineNanos))
                .map(publishedMessage -> Long.valueOf(publishedMessage.getId()))
                .toList();
    }

    // 발행에 실패하면 브로커와의 연결에 문제가 있는 것이라 남은 발송 기록은 발행하지 않고 다음 주기에 다시 시도한다.
    private List<CorrelationData> publishUntilFailure(List<Long> deliveryIds) {
        List<CorrelationData> publishedMessages = new ArrayList<>();
        for (Long deliveryId : deliveryIds) {
            CorrelationData correlationData = new CorrelationData(deliveryId.toString());
            try {
                rabbitTemplate.convertAndSend(
                        notificationQueueProperties.exchange(),
                        notificationQueueProperties.delivery(),
                        deliveryId.toString(),
                        correlationData);
            } catch (AmqpException e) {
                log.warn("[Notification] deliveryId={} 발행 실패, 남은 발송 기록은 다음 주기에 발행", deliveryId, e);
                break;
            }
            publishedMessages.add(correlationData);
        }
        return publishedMessages;
    }

    // 모든 메시지가 같은 기한을 나눠 쓰므로, 확인을 기다리는 시간은 건수와 관계없이 최대 CONFIRM_TIMEOUT_MILLIS다.
    // 브로커가 받았다고 확인했고, 큐로 라우팅되지 않아 돌아오지도 않았을 때만 발행에 성공한 것으로 본다.
    private boolean isRoutedToQueue(CorrelationData correlationData, long confirmDeadlineNanos) {
        try {
            CorrelationData.Confirm confirm =
                    correlationData
                            .getFuture()
                            .get(
                                    Math.max(0, confirmDeadlineNanos - System.nanoTime()),
                                    TimeUnit.NANOSECONDS);
            return confirm.ack() && correlationData.getReturned() == null;
        } catch (ExecutionException | TimeoutException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
