package com.symphonia.auth.infrastructure.oauth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.symphonia.auth.infrastructure.oauth.config.properties.GoogleOAuthProperties;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

// OAuthConfig.googleIdTokenDecoder()는 withJwkSetUri()로 실제 네트워크에서 JWK Set을 가져오므로
// 여기서는 withPublicKey() 기반 디코더에 OAuthConfig.googleIdTokenValidator()(프로덕션과 동일한 코드)를
// 그대로 붙여서 검증한다. 검증기 조합 로직이 바뀌면 이 테스트도 함께 반응한다.
@DisplayName("Google ID 토큰 검증기 단위 테스트")
class GoogleIdTokenDecoderTest {

    private static final String ISSUER = "https://accounts.google.com";
    private static final String CLIENT_ID = "test-client-id";
    private static final String SUBJECT = "google-subject-id";

    private RSAPrivateKey privateKey;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        GoogleOAuthProperties properties =
                new GoogleOAuthProperties(
                        CLIENT_ID,
                        "client-secret",
                        "redirect-uri",
                        "token-uri",
                        "jwks-uri",
                        ISSUER);
        decoder = createDecoder((RSAPublicKey) keyPair.getPublic(), properties);
    }

    @Nested
    @DisplayName("decode 메서드는")
    class Decode {

        @Nested
        @DisplayName("iss와 aud가 올바르고 만료되지 않은 토큰인 경우")
        class WhenTokenIsValid {

            @Test
            @DisplayName("Jwt로 디코딩한다.")
            void shouldDecodeJwt() {
                // given
                String token =
                        signedToken(ISSUER, List.of(CLIENT_ID), Instant.now().plusSeconds(300));

                // when
                Jwt jwt = decoder.decode(token);

                // then
                assertThat(jwt.getSubject()).isEqualTo(SUBJECT);
            }
        }

        @Nested
        @DisplayName("aud가 다른 토큰인 경우")
        class WhenAudienceMismatches {

            @Test
            @DisplayName("JwtException이 발생한다.")
            void shouldThrowJwtException() {
                // given
                String token =
                        signedToken(
                                ISSUER, List.of("other-client-id"), Instant.now().plusSeconds(300));

                // when & then
                assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
            }
        }

        @Nested
        @DisplayName("iss가 다른 토큰인 경우")
        class WhenIssuerMismatches {

            @Test
            @DisplayName("JwtException이 발생한다.")
            void shouldThrowJwtException() {
                // given
                String token =
                        signedToken(
                                "https://evil.example.com",
                                List.of(CLIENT_ID),
                                Instant.now().plusSeconds(300));

                // when & then
                assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
            }
        }

        @Nested
        @DisplayName("만료된 토큰인 경우")
        class WhenTokenExpired {

            @Test
            @DisplayName("JwtException이 발생한다.")
            void shouldThrowJwtException() {
                // given
                String token =
                        signedToken(ISSUER, List.of(CLIENT_ID), Instant.now().minusSeconds(300));

                // when & then
                assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
            }
        }
    }

    private String signedToken(String issuer, List<String> audience, Instant expiresAt) {
        try {
            JWTClaimsSet claims =
                    new JWTClaimsSet.Builder()
                            .issuer(issuer)
                            .audience(audience)
                            .subject(SUBJECT)
                            .issueTime(Date.from(Instant.now().minusSeconds(60)))
                            .expirationTime(Date.from(expiresAt))
                            .build();
            SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            signedJwt.sign(new RSASSASigner(privateKey));

            return signedJwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private JwtDecoder createDecoder(RSAPublicKey publicKey, GoogleOAuthProperties properties) {
        NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        nimbusJwtDecoder.setJwtValidator(OAuthConfig.googleIdTokenValidator(properties));

        return nimbusJwtDecoder;
    }
}
