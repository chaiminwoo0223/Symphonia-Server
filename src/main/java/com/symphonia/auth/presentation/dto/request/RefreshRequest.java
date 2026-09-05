package com.symphonia.auth.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank @Schema(description = "리프레시 토큰") String refreshToken) {}
