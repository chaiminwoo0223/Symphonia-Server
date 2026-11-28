package com.symphonia.gathering.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SeenShareRequest(
        @NotNull @Schema(description = "분담금이 속한 정산 건 ID") Long settlementId,
        @NotNull @PositiveOrZero @Schema(description = "화면에서 본 분담금 (원)") Long amount) {}
