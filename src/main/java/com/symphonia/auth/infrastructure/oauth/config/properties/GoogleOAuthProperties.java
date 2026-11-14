package com.symphonia.auth.infrastructure.oauth.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.google")
public record GoogleOAuthProperties(String clientId, String jwksUri, List<String> issuers) {}
