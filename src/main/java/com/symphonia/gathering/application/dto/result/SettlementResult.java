package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.Settlement;
import java.time.LocalDateTime;
import java.util.List;

public record SettlementResult(
        Long id,
        Long payerParticipantId,
        long totalAmount,
        long payerAmount,
        LocalDateTime recordedAt,
        boolean notified,
        List<SettlementShareResult> shares) {
    public static SettlementResult from(Settlement settlement) {
        return new SettlementResult(
                settlement.getId(),
                settlement.getPayerParticipantId(),
                settlement.getTotalAmount(),
                settlement.getPayerAmount(),
                settlement.getRecordedAt(),
                settlement.isNotified(),
                settlement.getShares().stream().map(SettlementShareResult::from).toList());
    }
}
