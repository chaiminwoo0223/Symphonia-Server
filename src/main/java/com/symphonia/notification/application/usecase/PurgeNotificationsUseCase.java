package com.symphonia.notification.application.usecase;

import java.util.Collection;

public interface PurgeNotificationsUseCase {
    // 파기하는 모임의 발송 예약과 발송 기록을 지운다. gathering이 모임을 지우는 트랜잭션에서 모임보다 먼저 부른다(#128).
    void purge(Collection<Long> gatheringIds);
}
