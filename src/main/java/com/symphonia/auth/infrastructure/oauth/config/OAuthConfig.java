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
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({KakaoOAuthProperties.class, GoogleOAuthProperties.class})
public class OAuthConfig {
    // 소셜 제공자 응답 지연이 서버 스레드를 붙잡아 두지 않도록 명시적으로 제한한다.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder().requestFactory(requestFactory).build();
    }

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
        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(properties.issuer());
        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<List<String>>(
                        "aud", aud -> aud != null && aud.contains(properties.clientId()));

        return new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator);
    }
}
