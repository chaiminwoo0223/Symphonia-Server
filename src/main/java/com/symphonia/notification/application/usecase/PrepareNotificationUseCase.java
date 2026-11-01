package com.symphonia.notification.application.usecase;

import com.symphonia.notification.application.dto.result.PreparedNotificationResult;
import java.util.Optional;

public interface PrepareNotificationUseCase {
    // 보낼 메시지가 없으면(이미 보냄, 다른 컨슈머가 맡음, 시간대 밖이라 미룸, 담을 금액 없음) 비어 있다.
    Optional<PreparedNotificationResult> prepare(Long notificationDeliveryId);
}
