package com.symphonia.auth.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

// 로그아웃은 토큰이 없어도 204를 반환하므로 refreshToken은 비어 있을 수 있다.
public record LogoutRequest(
        @Size(max = 100) @Schema(description = "리프레시 토큰", nullable = true) String refreshToken) {}
