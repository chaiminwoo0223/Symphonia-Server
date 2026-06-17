package com.symphonia.global.config;

import com.symphonia.global.config.properties.TokenProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableWebSecurity
@Configuration
@EnableConfigurationProperties(TokenProperties.class)
public class SecurityConfig {
}
