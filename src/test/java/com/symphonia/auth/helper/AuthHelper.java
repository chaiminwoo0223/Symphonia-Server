package com.symphonia.auth.helper;

import com.symphonia.auth.infrastructure.provider.AccessTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthHelper {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenProvider accessTokenProvider;

    public String generateAccessToken(String memberId, String role) {
        return accessTokenProvider.generate(memberId, role);
    }

    public String bearerHeader(String accessToken) {
        return BEARER_PREFIX + accessToken;
    }
}
