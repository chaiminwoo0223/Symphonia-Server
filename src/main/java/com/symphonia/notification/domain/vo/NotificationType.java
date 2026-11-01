package com.symphonia.notification.domain.vo;

// SETTLEMENT는 보낼 분담금을 알리는 정산 메시지이고, CANCELLATION은 정산 메시지를 받은 정산 건이 취소됐다는 알림이다.
public enum NotificationType {
    SETTLEMENT,
    CANCELLATION
}
