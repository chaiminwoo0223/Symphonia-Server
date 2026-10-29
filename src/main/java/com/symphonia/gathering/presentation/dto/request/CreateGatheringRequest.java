package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.CreateGatheringCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGatheringRequest(
        @NotBlank @Size(max = 50) @Schema(description = "모임 이름") String name) {
    public CreateGatheringCommand toCommand(String memberId) {
        return new CreateGatheringCommand(Long.parseLong(memberId), name);
    }
}
