package com.symphonia.auth.presentation.dto.request;

import com.symphonia.auth.application.dto.command.RefreshCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @NotBlank @Size(max = 100) @Schema(description = "리프레시 토큰") String refreshToken) {
    public RefreshCommand toCommand(String ip) {
        return RefreshCommand.of(refreshToken, ip);
    }
}
