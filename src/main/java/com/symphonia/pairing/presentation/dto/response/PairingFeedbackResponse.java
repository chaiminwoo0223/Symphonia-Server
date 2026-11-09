package com.symphonia.pairing.presentation.dto.response;

import com.symphonia.pairing.application.dto.result.PairingFeedbackResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record PairingFeedbackResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "술 ID") Long drinkId,
        @Schema(description = "안주 ID") Long anjuId,
        @Schema(description = "관계 유형") String relationshipType,
        @Schema(description = "분위기 유형. 분위기 없이 추천받은 조합이면 null") String moodType,
        @Schema(description = "평가값") String rating) {
    public static PairingFeedbackResponse from(PairingFeedbackResult result) {
        return new PairingFeedbackResponse(
                result.id(),
                result.drinkId(),
                result.anjuId(),
                result.relationshipType().name(),
                result.moodType() == null ? null : result.moodType().name(),
                result.rating().name());
    }
}
