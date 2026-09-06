package com.symphonia.auth.presentation.controller;

import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.usecase.LogoutUseCase;
import com.symphonia.auth.application.usecase.ReissueUseCase;
import com.symphonia.auth.presentation.AuthApi;
import com.symphonia.auth.presentation.dto.request.RefreshRequest;
import com.symphonia.auth.presentation.dto.response.TokenResponse;
import com.symphonia.common.response.StandardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final ReissueUseCase reissueUseCase;
    private final LogoutUseCase logoutUseCase;

    @Override
    public ResponseEntity<StandardResponse<TokenResponse>> refresh(RefreshRequest request) {
        TokenResult result = reissueUseCase.reissue(request.refreshToken());
        TokenResponse response = TokenResponse.from(result);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<Void>> logout(Authentication authentication) {
        String accessToken = (String) authentication.getCredentials();
        logoutUseCase.logout(accessToken);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(StandardResponse.success(HttpStatus.NO_CONTENT));
    }
}
