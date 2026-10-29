package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.OpenGatheringCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OpenGatheringRequest(
        @NotBlank @Size(max = 50) @Schema(description = "모임 이름") String name) {
    public OpenGatheringCommand toCommand(String memberId) {
        return new OpenGatheringCommand(Long.parseLong(memberId), name);
    }
}
