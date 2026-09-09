package com.symphonia.auth.presentation.controller;

import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.usecase.LoginUseCase;
import com.symphonia.auth.application.usecase.LogoutUseCase;
import com.symphonia.auth.application.usecase.ReissueUseCase;
import com.symphonia.auth.application.usecase.SignupUseCase;
import com.symphonia.auth.presentation.AuthApi;
import com.symphonia.auth.presentation.cookie.CookieProvider;
import com.symphonia.auth.presentation.dto.request.LoginRequest;
import com.symphonia.auth.presentation.dto.request.SignupRequest;
import com.symphonia.auth.presentation.dto.response.TokenResponse;
import com.symphonia.common.response.StandardResponse;
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
    private final ReissueUseCase reissueUseCase;
    private final LogoutUseCase logoutUseCase;
    private final CookieProvider cookieProvider;

    @Override
    public ResponseEntity<StandardResponse<TokenResponse>> signup(SignupRequest request) {
        TokenResult result = signupUseCase.signup(request.toCommand());
        TokenResponse response = TokenResponse.from(result);
        ResponseCookie cookie = cookieProvider.create(result.refreshToken());

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(StandardResponse.success(HttpStatus.CREATED, response));
    }

    @Override
    public ResponseEntity<StandardResponse<TokenResponse>> login(LoginRequest request) {
        TokenResult result = loginUseCase.login(request.toCommand());
        TokenResponse response = TokenResponse.from(result);
        ResponseCookie cookie = cookieProvider.create(result.refreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<TokenResponse>> refresh(String refreshToken) {
        TokenResult result = reissueUseCase.reissue(refreshToken);
        TokenResponse response = TokenResponse.from(result);
        ResponseCookie cookie = cookieProvider.create(result.refreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<Void>> logout(Authentication authentication) {
        String accessToken = (String) authentication.getCredentials();
        logoutUseCase.logout(accessToken);
        ResponseCookie cookie = cookieProvider.expire();

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(StandardResponse.success(HttpStatus.NO_CONTENT));
    }
}
