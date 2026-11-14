package com.symphonia.auth.presentation.dto.request;

import com.symphonia.auth.application.dto.command.LoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 20) @Schema(description = "소셜 로그인 제공자 (kakao, google)")
                String provider,
        @NotBlank @Size(max = 4096) @Schema(description = "소셜 SDK 토큰 (카카오는 액세스 토큰, 구글은 ID 토큰)")
                String token) {
    public LoginCommand toCommand(String ip) {
        return new LoginCommand(provider, token, ip);
    }
}
