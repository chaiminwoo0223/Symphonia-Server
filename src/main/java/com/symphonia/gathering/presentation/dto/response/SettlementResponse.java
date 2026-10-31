package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.SettlementResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record SettlementResponse(
        @Schema(description = "정산 건 ID") Long id,
        @Schema(description = "결제자 참여자 ID") Long payerParticipantId,
        @Schema(description = "카드 결제 총액 (원)") long totalAmount,
        @Schema(description = "결제자 분담액 (원)") long payerAmount,
        @Schema(description = "결제자가 받았다고 확인한 금액 (원)") long confirmedAmount,
        @Schema(description = "참여자가 보냈다고 알렸지만 결제자가 아직 확인하지 않은 금액 (원)") long sentAmount,
        @Schema(description = "마지막으로 기록하거나 수정한 시각") LocalDateTime recordedAt,
        @Schema(description = "정산 메시지 발송 여부 (보냈으면 수정 불가)") boolean notified,
        @Schema(description = "결제자를 뺀 참여자의 분담금") List<SettlementShareResponse> shares) {
    public static SettlementResponse from(SettlementResult result) {
        return new SettlementResponse(
                result.id(),
                result.payerParticipantId(),
                result.totalAmount(),
                result.payerAmount(),
                result.confirmedAmount(),
                result.sentAmount(),
                result.recordedAt(),
                result.notified(),
                result.shares().stream().map(SettlementShareResponse::from).toList());
    }
}
