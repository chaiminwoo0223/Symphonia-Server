package com.symphonia.auth.infrastructure.provider;

import com.symphonia.global.config.properties.RefreshTokenProperties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
