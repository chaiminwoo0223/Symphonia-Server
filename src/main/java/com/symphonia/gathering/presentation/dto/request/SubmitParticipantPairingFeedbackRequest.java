package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.SubmitParticipantPairingFeedbackCommand;
import com.symphonia.pairing.domain.vo.PairingRating;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record SubmitParticipantPairingFeedbackRequest(
        @NotNull @Schema(description = "평가값") PairingRating rating) {
    public SubmitParticipantPairingFeedbackCommand toCommand(String linkToken) {
        return new SubmitParticipantPairingFeedbackCommand(linkToken, rating);
    }
}
