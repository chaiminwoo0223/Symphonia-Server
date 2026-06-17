package com.symphonia.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record TokenProperties(
        String secret,
        Long accessExpirationTime,
        Long refreshExpirationTime
) {
}
