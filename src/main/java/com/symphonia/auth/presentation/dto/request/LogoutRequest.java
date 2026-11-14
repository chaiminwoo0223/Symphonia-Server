package com.symphonia.auth.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

// 로그아웃은 어떤 토큰이 와도 204를 반환하므로 refreshToken은 비어 있거나 길이 검증 없이 받는다.
public record LogoutRequest(
        @Schema(description = "리프레시 토큰", nullable = true) String refreshToken) {}
