package com.symphonia.pairing.presentation.dto.request;

import com.symphonia.pairing.application.dto.command.SubmitPairingFeedbackCommand;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingRating;
import com.symphonia.pairing.domain.vo.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record SubmitPairingFeedbackRequest(
        @NotNull @Schema(description = "술 ID") Long drinkId,
        @NotNull @Schema(description = "안주 ID") Long anjuId,
        @NotNull @Schema(description = "관계 유형") RelationshipType relationshipType,
        @Schema(description = "추천받을 때 고른 분위기 유형(선택)") MoodType moodType,
        @NotNull @Schema(description = "평가값") PairingRating rating) {
    public SubmitPairingFeedbackCommand toCommand(String memberId) {
        return new SubmitPairingFeedbackCommand(
                Long.parseLong(memberId), drinkId, anjuId, relationshipType, moodType, rating);
    }
}
