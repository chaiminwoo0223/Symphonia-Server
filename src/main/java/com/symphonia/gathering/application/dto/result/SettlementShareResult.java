package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.SettlementShare;
import com.symphonia.gathering.domain.vo.PaymentStatus;

public record SettlementShareResult(
        Long participantId, long amount, boolean fixed, PaymentStatus paymentStatus) {
    public static SettlementShareResult from(SettlementShare share) {
        return new SettlementShareResult(
                share.getParticipantId(),
                share.getAmount(),
                share.isFixed(),
                share.getPaymentStatus());
    }
}
