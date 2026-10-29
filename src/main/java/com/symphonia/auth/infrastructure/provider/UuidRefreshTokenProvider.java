package com.symphonia.auth.infrastructure.provider;

import com.symphonia.auth.domain.provider.RefreshTokenProvider;
import com.symphonia.global.config.properties.RefreshTokenProperties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UuidRefreshTokenProvider implements RefreshTokenProvider {
    private final RefreshTokenProperties refreshTokenProperties;

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }

    @Override
    public long getExpirationTime() {
        return refreshTokenProperties.expirationTime();
    }
}
