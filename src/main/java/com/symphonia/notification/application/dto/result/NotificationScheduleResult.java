package com.symphonia.notification.application.dto.result;

import com.symphonia.notification.domain.entity.NotificationSchedule;
import java.time.LocalDateTime;

// scheduledAt은 보낼 정산 건이 있을 때만 값이 있다. 결제자는 이 시각까지 정산 건을 고칠 수 있다.
public record NotificationScheduleResult(LocalDateTime scheduledAt) {
    public static NotificationScheduleResult from(NotificationSchedule notificationSchedule) {
        return new NotificationScheduleResult(notificationSchedule.getScheduledAt());
    }

    public static NotificationScheduleResult empty() {
        return new NotificationScheduleResult(null);
    }
}
