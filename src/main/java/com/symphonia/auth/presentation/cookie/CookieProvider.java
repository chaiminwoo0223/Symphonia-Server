package com.symphonia.auth.presentation.cookie;

import com.symphonia.global.config.properties.RefreshTokenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieProvider {

    public static final String COOKIE_NAME = "refresh_token";

    private static final String COOKIE_PATH = "/api/v1/auth/refresh";

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
