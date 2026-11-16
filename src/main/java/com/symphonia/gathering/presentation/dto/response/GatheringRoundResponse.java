package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.GatheringRoundResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record GatheringRoundResponse(
        @Schema(description = "차수 ID") Long id, @Schema(description = "차수 번호 (1차면 1)") int number) {
    public static GatheringRoundResponse from(GatheringRoundResult result) {
        return new GatheringRoundResponse(result.id(), result.number());
    }
}
