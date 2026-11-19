package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.AddRoundCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

public record AddRoundRequest(@Positive @Schema(description = "추가할 차수의 번호") int roundNumber) {
    public AddRoundCommand toCommand(Long memberId, Long gatheringId) {
        return AddRoundCommand.of(memberId, gatheringId, roundNumber);
    }
}
