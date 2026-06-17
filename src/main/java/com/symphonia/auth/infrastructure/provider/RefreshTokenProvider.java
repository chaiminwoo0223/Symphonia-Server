package com.symphonia.auth.infrastructure.provider;

import com.symphonia.global.config.properties.RefreshTokenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefreshTokenProvider {
    private final RefreshTokenProperties refreshTokenProperties;

    public String generate() {
        return UUID.randomUUID().toString();
    }

    public long getExpirationTime() {
        return refreshTokenProperties.expirationTime();
    }
}
