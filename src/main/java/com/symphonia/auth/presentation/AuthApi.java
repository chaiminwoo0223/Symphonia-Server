package com.symphonia.auth.presentation;

import com.symphonia.auth.presentation.dto.request.RefreshRequest;
import com.symphonia.auth.presentation.dto.response.TokenResponse;
import com.symphonia.global.common.response.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/auth")
@Tag(name = "Auth API", description = "인증 API")
public interface AuthApi {

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "새로운 엑세스 토큰과 리프레시 토큰을 재발급합니다.")
    ResponseEntity<StandardResponse<TokenResponse>> refresh(
            @RequestBody @Valid RefreshRequest request);

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "인증된 멤버를 로그아웃 처리합니다.")
    ResponseEntity<StandardResponse<Void>> logout(Authentication authentication);
}
