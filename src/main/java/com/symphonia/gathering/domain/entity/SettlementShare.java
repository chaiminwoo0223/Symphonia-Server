package com.symphonia.gathering.domain.entity;

import com.symphonia.gathering.domain.vo.PaymentStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 결제자를 뺀 참여자 한 명의 분담이다. fixed는 결제자가 금액을 지정했는지를 나타내고, 계산 근거를 보여 줄 때 쓴다.
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SettlementShare {
    private Long participantId;
    private long amount;
    private boolean fixed;
    private PaymentStatus paymentStatus;

    public static SettlementShare equal(Long participantId, long amount) {
        return of(participantId, amount, false);
    }

    public static SettlementShare fixed(Long participantId, long amount) {
        return of(participantId, amount, true);
    }

    public boolean isUnpaid() {
        return paymentStatus == PaymentStatus.UNPAID;
    }

    private static SettlementShare of(Long participantId, long amount, boolean fixed) {
        return SettlementShare.builder()
                .participantId(participantId)
                .amount(amount)
                .fixed(fixed)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();
    }
}
