package com.symphonia.pairing.presentation.dto.request;

import com.symphonia.pairing.application.dto.command.SubmitPairingFeedbackCommand;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record SubmitPairingFeedbackRequest(
        @NotNull @Schema(description = "술 ID") Long drinkId,
        @NotNull @Schema(description = "안주 ID") Long anjuId,
        @NotNull @Schema(description = "음악 무드 ID") Long musicMoodId,
        @NotNull @Schema(description = "관계 유형") RelationshipType relationshipType,
        @NotNull @Schema(description = "분위기 유형") MoodType moodType) {
    public SubmitPairingFeedbackCommand toCommand(String memberId) {
        return new SubmitPairingFeedbackCommand(
                Long.parseLong(memberId), drinkId, anjuId, musicMoodId, relationshipType, moodType);
    }
}
