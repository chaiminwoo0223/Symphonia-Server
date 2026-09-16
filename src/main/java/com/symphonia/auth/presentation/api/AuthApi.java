package com.symphonia.auth.presentation.api;

import com.symphonia.auth.presentation.AuthEndpoints;
import com.symphonia.auth.presentation.cookie.CookieProvider;
import com.symphonia.auth.presentation.dto.request.LoginRequest;
import com.symphonia.auth.presentation.dto.request.SignupRequest;
import com.symphonia.auth.presentation.dto.response.TokenResponse;
import com.symphonia.common.response.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth API", description = "인증 API")
public interface AuthApi {
    @PostMapping(AuthEndpoints.SIGNUP)
    @Operation(summary = "회원가입", description = "소셜 로그인 인가 코드로 회원가입을 처리하고 토큰을 발급합니다.")
    ResponseEntity<StandardResponse<TokenResponse>> signup(
            @Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest);

    @PostMapping(AuthEndpoints.LOGIN)
    @Operation(summary = "로그인", description = "소셜 로그인 인가 코드로 로그인을 처리하고 토큰을 발급합니다.")
    ResponseEntity<StandardResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest);

    @PostMapping(AuthEndpoints.REFRESH)
    @Operation(summary = "토큰 재발급", description = "리프레시 토큰 쿠키로 새로운 엑세스 토큰과 리프레시 토큰을 재발급합니다.")
    ResponseEntity<StandardResponse<TokenResponse>> refresh(
            @CookieValue(name = CookieProvider.COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest httpRequest);

    @PostMapping(AuthEndpoints.LOGOUT)
    @Operation(summary = "로그아웃", description = "인증된 멤버를 로그아웃 처리합니다.")
    ResponseEntity<StandardResponse<Void>> logout(
            Authentication authentication, HttpServletRequest httpRequest);
}
