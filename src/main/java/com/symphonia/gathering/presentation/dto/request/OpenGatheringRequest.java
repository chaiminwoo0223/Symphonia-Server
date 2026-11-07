package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.OpenGatheringCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OpenGatheringRequest(
        @NotBlank @Size(max = 50) @Schema(description = "모임 이름") String name,
        @Valid @Schema(description = "추천 결과에서 고른 페어링 (선택, 나중에 모임장이 고를 수 있음)")
                GatheringPairingRequest pairing) {
    public OpenGatheringCommand toCommand(String memberId) {
        return new OpenGatheringCommand(
                Long.parseLong(memberId), name, pairing == null ? null : pairing.toCommand());
    }
}
