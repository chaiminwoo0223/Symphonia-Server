package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.RenameGatheringCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameGatheringRequest(
        @NotBlank @Size(max = 50) @Schema(description = "바꿀 모임 이름") String name) {
    public RenameGatheringCommand toCommand(String memberId, Long gatheringId) {
        return RenameGatheringCommand.of(Long.parseLong(memberId), gatheringId, name);
    }
}
