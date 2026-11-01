package com.symphonia.notification.application.usecase;

import com.symphonia.notification.application.dto.command.RescheduleNotificationCommand;
import com.symphonia.notification.application.dto.result.NotificationScheduleResult;

public interface RescheduleNotificationUseCase {
    // 모임의 발송 예약을 잠그고 기록 시각으로 예약 시각을 다시 계산한다. 정산 건을 바꾸기 전에 같은 트랜잭션에서 부른다.
    NotificationScheduleResult reschedule(RescheduleNotificationCommand command);
}
