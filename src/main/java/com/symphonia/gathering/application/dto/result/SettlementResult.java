package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.vo.ShareStatus;
import java.time.LocalDateTime;
import java.util.List;

// confirmedAmount는 결제자가 받았다고 확인한 금액이고, sentAmount는 참여자가 보냈다고 알렸지만 아직 확인하지 않은 금액이다.
// notificationScheduledAt은 아직 보내지 않은 정산 건의 정산 메시지 발송 예정 시각이고, 그때까지 수정할 수 있다.
public record SettlementResult(
        Long id,
        Long payerParticipantId,
        long totalAmount,
        long payerAmount,
        long confirmedAmount,
        long sentAmount,
        LocalDateTime recordedAt,
        boolean notified,
        LocalDateTime notificationScheduledAt,
        List<SettlementShareResult> shares) {
    // 발송 예정 시각은 모임의 예약에 있으므로 함께 받는다. 이미 보낸 정산 건에는 담지 않는다.
    public static SettlementResult of(
            Settlement settlement, LocalDateTime notificationScheduledAt) {
        return new SettlementResult(
                settlement.getId(),
                settlement.getPayerParticipantId(),
                settlement.getTotalAmount(),
                settlement.getPayerAmount(),
                settlement.getAmountOf(ShareStatus.CONFIRMED),
                settlement.getAmountOf(ShareStatus.SENT),
                settlement.getRecordedAt(),
                settlement.isNotified(),
                settlement.isNotified() ? null : notificationScheduledAt,
                settlement.getShares().stream().map(SettlementShareResult::from).toList());
    }
}
