package com.symphonia.notification.application.dto.command;

import java.time.LocalDateTime;

// recordedAt은 정산 건을 기록하거나 수정한 시각이다. 예약 시각은 이 시각에 수정 시간을 더해 계산한다.
public record RescheduleNotificationCommand(Long gatheringId, LocalDateTime recordedAt) {
    public static RescheduleNotificationCommand of(Long gatheringId, LocalDateTime recordedAt) {
        return new RescheduleNotificationCommand(gatheringId, recordedAt);
    }
}
