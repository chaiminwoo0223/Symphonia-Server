package com.symphonia.auth.presentation.controller;

import com.symphonia.auth.application.dto.command.LogoutCommand;
import com.symphonia.auth.application.dto.command.RefreshCommand;
import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.usecase.LoginUseCase;
import com.symphonia.auth.application.usecase.LogoutUseCase;
import com.symphonia.auth.application.usecase.RefreshUseCase;
import com.symphonia.auth.application.usecase.SignupUseCase;
import com.symphonia.auth.presentation.api.AuthApi;
import com.symphonia.auth.presentation.cookie.RefreshTokenCookieFactory;
import com.symphonia.auth.presentation.dto.request.LoginRequest;
import com.symphonia.auth.presentation.dto.request.SignupRequest;
import com.symphonia.auth.presentation.dto.response.TokenResponse;
import com.symphonia.common.response.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final SignupUseCase signupUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshUseCase refreshUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    @Override
    public ResponseEntity<StandardResponse<TokenResponse>> signup(
            SignupRequest request, HttpServletRequest httpRequest) {
        TokenResult result = signupUseCase.signup(request.toCommand(extractIp(httpRequest)));
        TokenResponse response = TokenResponse.from(result);
        ResponseCookie cookie = refreshTokenCookieFactory.create(result.refreshToken());

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(StandardResponse.success(HttpStatus.CREATED, response));
    }

    @Override
    public ResponseEntity<StandardResponse<TokenResponse>> login(
            LoginRequest request, HttpServletRequest httpRequest) {
        TokenResult result = loginUseCase.login(request.toCommand(extractIp(httpRequest)));
        TokenResponse response = TokenResponse.from(result);
        ResponseCookie cookie = refreshTokenCookieFactory.create(result.refreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<TokenResponse>> refresh(
            String refreshToken, HttpServletRequest httpRequest) {
        RefreshCommand command = RefreshCommand.of(refreshToken, extractIp(httpRequest));
        TokenResult result = refreshUseCase.refresh(command);
        TokenResponse response = TokenResponse.from(result);
        ResponseCookie cookie = refreshTokenCookieFactory.create(result.refreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<Void>> logout(
            Authentication authentication, HttpServletRequest httpRequest) {
        String accessToken = (String) authentication.getCredentials();
        LogoutCommand command = LogoutCommand.of(accessToken, extractIp(httpRequest));
        logoutUseCase.logout(command);
        ResponseCookie cookie = refreshTokenCookieFactory.expire();

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(StandardResponse.success(HttpStatus.NO_CONTENT));
    }

    private String extractIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
