package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.GatheringParticipantResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record GatheringParticipantResponse(
        @Schema(description = "참여자 ID") Long id,
        @Schema(description = "참여자 이름") String name,
        @Schema(description = "마스킹한 휴대폰 번호") String maskedPhoneNumber) {
    public static GatheringParticipantResponse from(GatheringParticipantResult result) {
        return new GatheringParticipantResponse(
                result.id(), result.name(), result.maskedPhoneNumber());
    }
}
