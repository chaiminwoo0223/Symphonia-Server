package com.symphonia.notification.application.usecase;

import com.symphonia.notification.application.dto.result.NotificationScheduleResult;

public interface GetNotificationScheduleUseCase {
    // 모임의 발송 예정 시각을 돌려준다. 보낼 정산 건이 없으면 예약 시각이 비어 있다.
    NotificationScheduleResult getSchedule(Long gatheringId);
}
