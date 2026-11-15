package com.symphonia.auth.infrastructure.oauth.config;

import com.symphonia.auth.infrastructure.oauth.config.properties.GoogleOAuthProperties;
import com.symphonia.auth.infrastructure.oauth.config.properties.KakaoOAuthProperties;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@EnableConfigurationProperties({KakaoOAuthProperties.class, GoogleOAuthProperties.class})
public class OAuthConfig {
    @Bean
    public JwtDecoder googleIdTokenDecoder(GoogleOAuthProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwksUri()).build();
        decoder.setJwtValidator(googleIdTokenValidator(properties));

        return decoder;
    }

    // withPublicKey() 기반 디코더로 동일한 검증기 조합을 재사용할 수 있도록
    // (예: 실제 네트워크 없이 검증하는 테스트) 별도 메서드로 분리한다.
    public static OAuth2TokenValidator<Jwt> googleIdTokenValidator(
            GoogleOAuthProperties properties) {
        // 구글은 iss를 "https://accounts.google.com"과 "accounts.google.com" 두 형태로 발급할 수 있다.
        OAuth2TokenValidator<Jwt> timestampValidator = JwtValidators.createDefault();
        OAuth2TokenValidator<Jwt> issuerValidator =
                new JwtClaimValidator<Object>(
                        "iss", iss -> iss != null && properties.issuers().contains(iss.toString()));
        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<List<String>>(
                        "aud", aud -> aud != null && aud.contains(properties.clientId()));

        return new DelegatingOAuth2TokenValidator<>(
                timestampValidator, issuerValidator, audienceValidator);
    }
}
