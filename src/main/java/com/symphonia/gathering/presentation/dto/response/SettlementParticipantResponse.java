package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.SettlementParticipantResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record SettlementParticipantResponse(
        @Schema(description = "참여자 ID") Long id,
        @Schema(description = "참여자 이름") String name,
        @Schema(description = "마스킹한 휴대폰 번호") String maskedPhoneNumber) {
    public static SettlementParticipantResponse from(SettlementParticipantResult result) {
        return new SettlementParticipantResponse(
                result.id(), result.name(), result.maskedPhoneNumber());
    }
}
