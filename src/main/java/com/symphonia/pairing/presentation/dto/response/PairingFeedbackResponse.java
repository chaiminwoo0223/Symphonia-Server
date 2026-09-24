package com.symphonia.pairing.presentation.dto.response;

import com.symphonia.pairing.application.dto.result.PairingFeedbackResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record PairingFeedbackResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "술 ID") Long drinkId,
        @Schema(description = "안주 ID") Long anjuId,
        @Schema(description = "음악 무드 ID") Long musicMoodId,
        @Schema(description = "관계 유형") String relationshipType,
        @Schema(description = "분위기 유형") String moodType,
        @Schema(description = "평가값") String rating) {
    public static PairingFeedbackResponse from(PairingFeedbackResult result) {
        return new PairingFeedbackResponse(
                result.id(),
                result.drinkId(),
                result.anjuId(),
                result.musicMoodId(),
                result.relationshipType().name(),
                result.moodType().name(),
                result.rating().name());
    }
}
