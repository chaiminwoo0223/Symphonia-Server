package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.MarkSharesSentCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record MarkSharesSentRequest(
        @NotNull @Schema(description = "분담을 보낸 결제자의 참여자 ID") Long payerParticipantId) {
    public MarkSharesSentCommand toCommand(String linkToken) {
        return new MarkSharesSentCommand(linkToken, payerParticipantId);
    }
}
