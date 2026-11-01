package com.symphonia.notification.domain.vo;

// SKIPPED는 보내기 직전에 다시 보니 알릴 금액이 없어 보내지 않은 경우이고, FAILED는 재시도를 다 쓰고도 보내지 못한 경우다.
public enum DeliveryStatus {
    PENDING,
    SENT,
    SKIPPED,
    FAILED
}
