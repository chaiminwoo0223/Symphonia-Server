package com.symphonia.place.infrastructure.naver.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "naver.local-search")
public record NaverLocalSearchProperties(String clientId, String clientSecret, String uri) {}
