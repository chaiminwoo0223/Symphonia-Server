package com.symphonia.auth.presentation.dto.request;

import com.symphonia.auth.application.dto.command.SignupCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank @Schema(description = "소셜 로그인 제공자 (kakao, google)") String provider,
        @NotBlank @Schema(description = "소셜 로그인 인가 코드") String code) {
    public SignupCommand toCommand() {
        return new SignupCommand(provider, code);
    }
}
