package com.symphonia.auth.presentation.dto.response;

import com.symphonia.auth.application.dto.result.TokenResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
        @Schema(description = "엑세스 토큰") String accessToken,
        @Schema(description = "리프레시 토큰. 보안 저장소에 보관하고 재발급할 때마다 새 값으로 교체한다") String refreshToken) {
    public static TokenResponse from(TokenResult result) {
        return new TokenResponse(result.accessToken(), result.refreshToken());
    }
}
