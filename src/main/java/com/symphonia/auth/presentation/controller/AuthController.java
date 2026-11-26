package com.symphonia.auth.presentation.controller;

import com.symphonia.auth.application.dto.command.LogoutCommand;
import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.usecase.LoginUseCase;
import com.symphonia.auth.application.usecase.LogoutUseCase;
import com.symphonia.auth.application.usecase.RefreshUseCase;
import com.symphonia.auth.application.usecase.SignupUseCase;
import com.symphonia.auth.presentation.api.AuthApi;
import com.symphonia.auth.presentation.dto.request.LoginRequest;
import com.symphonia.auth.presentation.dto.request.LogoutRequest;
import com.symphonia.auth.presentation.dto.request.RefreshRequest;
import com.symphonia.auth.presentation.dto.request.SignupRequest;
import com.symphonia.auth.presentation.dto.response.TokenResponse;
import com.symphonia.common.response.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @Override
    public ResponseEntity<StandardResponse<TokenResponse>> signup(
            SignupRequest request, HttpServletRequest httpRequest) {
        TokenResult result = signupUseCase.signup(request.toCommand(httpRequest.getRemoteAddr()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(HttpStatus.CREATED, TokenResponse.from(result)));
    }

    @Override
    public ResponseEntity<StandardResponse<TokenResponse>> login(
            LoginRequest request, HttpServletRequest httpRequest) {
        TokenResult result = loginUseCase.login(request.toCommand(httpRequest.getRemoteAddr()));

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, TokenResponse.from(result)));
    }

    @Override
    public ResponseEntity<StandardResponse<TokenResponse>> refresh(
            RefreshRequest request, HttpServletRequest httpRequest) {
        TokenResult result = refreshUseCase.refresh(request.toCommand(httpRequest.getRemoteAddr()));

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, TokenResponse.from(result)));
    }

    @Override
    public ResponseEntity<StandardResponse<Void>> logout(
            LogoutRequest request, Authentication authentication, HttpServletRequest httpRequest) {
        String refreshToken = request == null ? null : request.refreshToken();
        String accessToken =
                authentication == null ? null : (String) authentication.getCredentials();

        LogoutCommand command =
                LogoutCommand.of(refreshToken, accessToken, httpRequest.getRemoteAddr());
        logoutUseCase.logout(command);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(StandardResponse.success(HttpStatus.NO_CONTENT));
    }
}
