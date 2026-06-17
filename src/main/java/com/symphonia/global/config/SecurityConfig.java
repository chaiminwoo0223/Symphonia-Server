package com.symphonia.global.config;

import com.symphonia.global.config.properties.AccessTokenProperties;
import com.symphonia.global.config.properties.RefreshTokenProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableWebSecurity
@Configuration
@EnableConfigurationProperties({AccessTokenProperties.class, RefreshTokenProperties.class})
public class SecurityConfig {
}
