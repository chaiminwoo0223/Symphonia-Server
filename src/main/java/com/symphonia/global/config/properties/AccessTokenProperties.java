package com.symphonia.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "access-token")
public record AccessTokenProperties(String secret, Long expirationTime) {}
