package com.symphonia.notification.presentation.listener;

import com.symphonia.notification.application.dto.command.SendNotificationCommand;
import com.symphonia.notification.application.usecase.PrepareNotificationUseCase;
import com.symphonia.notification.application.usecase.SendNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// 큐로 받은 발송 기록 id로 메시지를 준비하고 보낸다. 준비와 발송은 트랜잭션을 나눠, 링크 토큰을 커밋한 뒤에 보낸다.
// 처리하다 예외가 나면 재시도하고, 재시도를 다 쓰면 메시지를 DLQ로 옮긴다. 발송 기록은 DB에 남아 폴러가 다시 발행한다.
@Component
@RequiredArgsConstructor
public class NotificationDeliveryListener {
    private final PrepareNotificationUseCase prepareNotificationUseCase;
    private final SendNotificationUseCase sendNotificationUseCase;

    @RabbitListener(queues = "${notification.queue.delivery}")
    public void receive(String notificationDeliveryId) {
        prepareNotificationUseCase
                .prepare(Long.valueOf(notificationDeliveryId))
                .map(SendNotificationCommand::from)
                .ifPresent(sendNotificationUseCase::send);
    }
}
