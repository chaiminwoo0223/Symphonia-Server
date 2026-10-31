package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.SettlementShareResult;
import com.symphonia.gathering.domain.vo.ShareStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record SettlementShareResponse(
        @Schema(description = "참여자 ID") Long participantId,
        @Schema(description = "분담액 (원)") long amount,
        @Schema(description = "결제자가 금액을 지정했는지 여부") boolean fixed,
        @Schema(description = "분담금 상태") ShareStatus status) {
    public static SettlementShareResponse from(SettlementShareResult result) {
        return new SettlementShareResponse(
                result.participantId(), result.amount(), result.fixed(), result.status());
    }
}
