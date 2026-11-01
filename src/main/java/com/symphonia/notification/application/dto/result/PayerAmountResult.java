package com.symphonia.notification.application.dto.result;

// 메시지에 담는 결제자 한 명과 그 결제자에게 보낼 금액이다.
public record PayerAmountResult(String payerName, long amount) {
    public static PayerAmountResult of(String payerName, long amount) {
        return new PayerAmountResult(payerName, amount);
    }
}
