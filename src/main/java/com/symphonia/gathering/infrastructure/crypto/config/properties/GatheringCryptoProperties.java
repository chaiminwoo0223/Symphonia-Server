package com.symphonia.gathering.infrastructure.crypto.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gathering.crypto")
public record GatheringCryptoProperties(String masterKey) {}
