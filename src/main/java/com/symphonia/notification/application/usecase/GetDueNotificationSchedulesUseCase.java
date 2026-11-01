package com.symphonia.notification.application.usecase;

import java.util.List;

public interface GetDueNotificationSchedulesUseCase {
    // 정산 메시지 예약 시각이 지난 모임의 id를 돌려준다.
    List<Long> getDueGatheringIds();
}
