package com.symphonia.notification.application.usecase;

public interface PublishNotificationDeliveriesUseCase {
    // 상한만큼 찾아 모두 발행했으면 남은 발송 기록이 있을 수 있으므로 true를 돌려준다.
    boolean publishPending();
}
