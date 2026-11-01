package com.symphonia.gathering.infrastructure.messaging.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 발송 기록 id를 담는 큐와 처리하지 못한 메시지를 모으는 DLQ의 이름이다.
// 리스너도 같은 설정 키로 큐 이름을 읽으므로, 이름은 application.yaml 한 곳에서만 정한다.
@ConfigurationProperties(prefix = "gathering.notification.queue")
public record NotificationQueueProperties(
        String exchange, String delivery, String deadLetterExchange, String deadLetter) {}
