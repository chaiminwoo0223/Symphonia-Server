package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.ParticipantPairingResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record ParticipantPairingResponse(
        @Schema(description = "술 이름") String drinkName,
        @Schema(description = "안주 이름") String anjuName,
        @Schema(description = "이미 피드백을 남겼는지 여부 (true면 피드백을 묻지 않음)") boolean feedbackSubmitted) {
    public static ParticipantPairingResponse from(ParticipantPairingResult result) {
        return new ParticipantPairingResponse(
                result.drinkName(), result.anjuName(), result.feedbackSubmitted());
    }
}
