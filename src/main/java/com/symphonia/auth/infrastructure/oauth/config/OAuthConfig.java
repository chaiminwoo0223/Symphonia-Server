package com.symphonia.auth.infrastructure.oauth.config;

import com.symphonia.auth.infrastructure.oauth.config.properties.GoogleOAuthProperties;
import com.symphonia.auth.infrastructure.oauth.config.properties.KakaoOAuthProperties;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties({KakaoOAuthProperties.class, GoogleOAuthProperties.class})
public class OAuthConfig {
    // NimbusJwtDecoder의 JWK Set 요청 기본 타임아웃은 연결과 읽기 모두 30초다.
    // 구글이 지연되면 로그인 요청이 그만큼 기다리므로 짧게 제한해 빨리 실패시킨다.
    private static final Duration JWK_SET_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration JWK_SET_READ_TIMEOUT = Duration.ofSeconds(3);

    @Bean
    public JwtDecoder googleIdTokenDecoder(GoogleOAuthProperties properties) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(properties.jwksUri())
                        .restOperations(jwkSetRestOperations())
                        .build();
        decoder.setJwtValidator(googleIdTokenValidator(properties));

        return decoder;
    }

    // withPublicKey() 기반 디코더로 동일한 검증기 조합을 재사용할 수 있도록
    // (예: 실제 네트워크 없이 검증하는 테스트) 별도 메서드로 분리한다.
    // 구글은 iss를 "https://accounts.google.com"과 "accounts.google.com" 두 형태로 발급할 수 있다.
    public static OAuth2TokenValidator<Jwt> googleIdTokenValidator(
            GoogleOAuthProperties properties) {
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

    private static RestOperations jwkSetRestOperations() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(JWK_SET_CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(JWK_SET_READ_TIMEOUT);

        return new RestTemplate(requestFactory);
    }
}
