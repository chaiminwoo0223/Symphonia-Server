package com.symphonia.notification.domain.vo;

// 메시지에 담는 결제자 한 명과 그 결제자에게 보낼 금액이다.
public record PayerAmount(String payerName, long amount) {}
