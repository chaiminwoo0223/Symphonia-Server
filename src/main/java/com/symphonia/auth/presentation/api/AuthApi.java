package com.symphonia.auth.presentation.api;

import com.symphonia.auth.presentation.AuthEndpoints;
import com.symphonia.auth.presentation.dto.request.LoginRequest;
import com.symphonia.auth.presentation.dto.request.LogoutRequest;
import com.symphonia.auth.presentation.dto.request.RefreshRequest;
import com.symphonia.auth.presentation.dto.request.SignupRequest;
import com.symphonia.auth.presentation.dto.response.TokenResponse;
import com.symphonia.common.response.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth API", description = "인증 API")
public interface AuthApi {
    @PostMapping(AuthEndpoints.SIGNUP)
    @Operation(
            summary = "회원가입",
            description = "카카오 액세스 토큰이나 구글 ID 토큰으로 회원가입을 처리하고 액세스 토큰과 리프레시 토큰을 발급합니다.")
    ResponseEntity<StandardResponse<TokenResponse>> signup(
            @Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest);

    @PostMapping(AuthEndpoints.LOGIN)
    @Operation(
            summary = "로그인",
            description = "카카오 액세스 토큰이나 구글 ID 토큰으로 로그인을 처리하고 액세스 토큰과 리프레시 토큰을 발급합니다.")
    ResponseEntity<StandardResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest);

    @PostMapping(AuthEndpoints.REFRESH)
    @Operation(
            summary = "토큰 재발급",
            description = "리프레시 토큰으로 새로운 액세스 토큰과 리프레시 토큰을 재발급합니다. 사용한 리프레시 토큰은 더 이상 쓸 수 없습니다.")
    ResponseEntity<StandardResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest);

    @PostMapping(AuthEndpoints.LOGOUT)
    @Operation(
            summary = "로그아웃",
            description =
                    "리프레시 토큰으로 이 기기의 세션을 끊고, 유효한 액세스 토큰이 있으면 함께 무효화합니다."
                            + " 토큰이 없거나 만료돼도 204를 반환합니다.")
    ResponseEntity<StandardResponse<Void>> logout(
            @Valid @RequestBody(required = false) @Nullable LogoutRequest request,
            @Nullable Authentication authentication,
            HttpServletRequest httpRequest);
}
