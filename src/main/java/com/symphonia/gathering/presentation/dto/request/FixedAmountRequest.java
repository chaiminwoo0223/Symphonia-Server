package com.symphonia.gathering.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record FixedAmountRequest(
        @NotNull @Schema(description = "금액을 지정할 참여자 ID") Long participantId,
        @PositiveOrZero @Max(100_000_000) @Schema(description = "지정 금액 (원)") long amount) {}
