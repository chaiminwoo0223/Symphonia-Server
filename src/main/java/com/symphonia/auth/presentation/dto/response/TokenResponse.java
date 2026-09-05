package com.symphonia.auth.presentation.dto.response;

import com.symphonia.auth.application.dto.result.TokenResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
        @Schema(description = "엑세스 토큰") String accessToken,
        @Schema(description = "리프레시 토큰") String refreshToken) {
    public static TokenResponse from(TokenResult result) {
        return new TokenResponse(result.accessToken(), result.refreshToken());
    }
}
