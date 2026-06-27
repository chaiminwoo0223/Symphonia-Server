package com.symphonia.auth.infrastructure.oauth.config;

import com.symphonia.auth.infrastructure.oauth.config.properties.KakaoOAuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KakaoOAuthProperties.class)
public class OAuthConfig {
}
