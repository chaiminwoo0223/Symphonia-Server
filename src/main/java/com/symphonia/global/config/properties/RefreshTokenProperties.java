package com.symphonia.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "refresh-token")
public record RefreshTokenProperties(
        Long expirationTime
) {
}
