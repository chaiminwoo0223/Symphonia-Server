package com.symphonia.gathering.infrastructure.messaging.config;

import com.symphonia.gathering.infrastructure.messaging.config.properties.NotificationQueueProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 브로커가 재시작돼도 메시지가 남도록 큐를 durable quorum queue로 둔다.
// 리스너가 재시도를 다 쓰고도 처리하지 못한 메시지는 DLQ로 옮긴다.
// 발송 기록은 DB에 남아 있어 폴러가 다시 발행하므로, DLQ는 복구 수단이 아니라 예상하지 못한 실패를 추적하는 기록이다.
@Configuration
@EnableConfigurationProperties(NotificationQueueProperties.class)
public class NotificationQueueConfig {
    @Bean
    public DirectExchange notificationExchange(NotificationQueueProperties properties) {
        return new DirectExchange(properties.exchange());
    }

    @Bean
    public Queue notificationDeliveryQueue(NotificationQueueProperties properties) {
        return QueueBuilder.durable(properties.delivery())
                .quorum()
                .deadLetterExchange(properties.deadLetterExchange())
                .deadLetterRoutingKey(properties.deadLetter())
                .build();
    }

    @Bean
    public Binding notificationDeliveryBinding(
            NotificationQueueProperties properties,
            DirectExchange notificationExchange,
            Queue notificationDeliveryQueue) {
        return BindingBuilder.bind(notificationDeliveryQueue)
                .to(notificationExchange)
                .with(properties.delivery());
    }

    @Bean
    public DirectExchange notificationDeadLetterExchange(NotificationQueueProperties properties) {
        return new DirectExchange(properties.deadLetterExchange());
    }

    @Bean
    public Queue notificationDeadLetterQueue(NotificationQueueProperties properties) {
        return QueueBuilder.durable(properties.deadLetter()).quorum().build();
    }

    @Bean
    public Binding notificationDeadLetterBinding(
            NotificationQueueProperties properties,
            DirectExchange notificationDeadLetterExchange,
            Queue notificationDeadLetterQueue) {
        return BindingBuilder.bind(notificationDeadLetterQueue)
                .to(notificationDeadLetterExchange)
                .with(properties.deadLetter());
    }
}
