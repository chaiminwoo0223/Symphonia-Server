package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.vo.ShareStatus;
import java.time.LocalDateTime;
import java.util.List;

// confirmedAmount는 결제자가 받았다고 확인한 금액이고, sentAmount는 참여자가 보냈다고 알렸지만 아직 확인하지 않은 금액이다.
public record SettlementResult(
        Long id,
        Long payerParticipantId,
        long totalAmount,
        long payerAmount,
        long confirmedAmount,
        long sentAmount,
        LocalDateTime recordedAt,
        boolean notified,
        List<SettlementShareResult> shares) {
    public static SettlementResult from(Settlement settlement) {
        return new SettlementResult(
                settlement.getId(),
                settlement.getPayerParticipantId(),
                settlement.getTotalAmount(),
                settlement.getPayerAmount(),
                settlement.getAmountOf(ShareStatus.CONFIRMED),
                settlement.getAmountOf(ShareStatus.SENT),
                settlement.getRecordedAt(),
                settlement.isNotified(),
                settlement.getShares().stream().map(SettlementShareResult::from).toList());
    }
}
