package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.ShareBreakdownResult;
import com.symphonia.gathering.domain.vo.ShareStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ShareBreakdownResponse(
        @Schema(description = "정산 건 ID") Long settlementId,
        @Schema(description = "카드 결제 총액 (원)") long totalAmount,
        @Schema(description = "결제자를 포함한 인원") int headcount,
        @Schema(description = "결제자가 이 참여자의 금액을 지정했는지 여부 (아니면 똑같이 나눈 금액)") boolean fixed,
        @Schema(description = "본인 분담액 (원)") long amount,
        @Schema(description = "분담금 상태") ShareStatus status,
        @Schema(description = "정산 건을 마지막으로 기록하거나 수정한 시각") LocalDateTime recordedAt) {
    public static ShareBreakdownResponse from(ShareBreakdownResult result) {
        return new ShareBreakdownResponse(
                result.settlementId(),
                result.totalAmount(),
                result.headcount(),
                result.fixed(),
                result.amount(),
                result.status(),
                result.recordedAt());
    }
}
