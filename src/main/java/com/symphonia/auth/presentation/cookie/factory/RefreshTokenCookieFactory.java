package com.symphonia.auth.presentation.cookie.factory;

import com.symphonia.auth.presentation.AuthEndpoints;
import com.symphonia.global.config.properties.RefreshTokenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieFactory {
    public static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = AuthEndpoints.REFRESH;

    private final RefreshTokenProperties refreshTokenProperties;

    public ResponseCookie create(String refreshToken) {
        return build(refreshToken, refreshTokenProperties.expirationTime());
    }

    public ResponseCookie expire() {
        return build("", 0);
    }

    private ResponseCookie build(String value, long maxAgeSeconds) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(refreshTokenProperties.cookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }
}
