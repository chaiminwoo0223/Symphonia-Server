package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.SettlementParticipantResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record SettlementParticipantResponse(
        @Schema(description = "참여자 ID") Long id, @Schema(description = "참여자 이름") String name) {
    public static SettlementParticipantResponse from(SettlementParticipantResult result) {
        return new SettlementParticipantResponse(result.id(), result.name());
    }
}
