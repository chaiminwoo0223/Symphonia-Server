package com.symphonia.notification.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.notification.application.dto.command.SendNotificationCommand;
import com.symphonia.notification.application.usecase.SendNotificationUseCase;
import com.symphonia.notification.domain.entity.NotificationDelivery;
import com.symphonia.notification.domain.exception.NotificationSendFailedException;
import com.symphonia.notification.domain.repository.NotificationDeliveryRepository;
import com.symphonia.notification.domain.sender.NotificationSender;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

// 준비한 메시지를 보내고 결과를 남긴다. 발송 기록을 잠그고 보내므로, 같은 발송 기록을 두 컨슈머가 동시에 보내지 않는다.
// 외부 발송이 실패하면 시도 횟수를 남기고 기다렸다가 폴러가 다시 발행한다. 큐의 재시도로 넘기지 않아 서버가 재시작돼도 이어진다.
@CommandService
@RequiredArgsConstructor
public class SendNotificationService implements SendNotificationUseCase {
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationSender notificationSender;
    private final Clock clock;

    // 준비한 뒤 모임을 파기해 발송 기록이 없어졌거나, 맡은 시간이 지나 다른 컨슈머가 먼저 끝냈으면 보내지 않는다.
    @Override
    public void send(SendNotificationCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        notificationDeliveryRepository
                .findByIdForUpdate(command.notificationDeliveryId())
                .filter(NotificationDelivery::isPending)
                .ifPresent(
                        notificationDelivery -> {
                            sendOrRetryLater(notificationDelivery, command, now);
                            notificationDeliveryRepository.save(notificationDelivery);
                        });
    }

    private void sendOrRetryLater(
            NotificationDelivery notificationDelivery,
            SendNotificationCommand command,
            LocalDateTime now) {
        try {
            notificationSender.send(command.toMessage());
            notificationDelivery.markSent(now);
        } catch (NotificationSendFailedException e) {
            notificationDelivery.retryLater(now);
        }
    }
}
