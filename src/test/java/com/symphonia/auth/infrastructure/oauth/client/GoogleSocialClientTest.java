package com.symphonia.auth.infrastructure.oauth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.symphonia.UnitTest;
import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.auth.domain.exception.InvalidSocialTokenException;
import com.symphonia.auth.domain.exception.SocialTokenVerificationUnavailableException;
import com.symphonia.auth.domain.identity.SocialIdentity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@DisplayName("GoogleSocialClient 단위 테스트")
class GoogleSocialClientTest extends UnitTest {

    private static final String ID_TOKEN = "id-token-value";

    @InjectMocks private GoogleSocialClient googleSocialClient;

    @Mock private JwtDecoder googleIdTokenDecoder;

    @Nested
    @DisplayName("authenticate 메서드는")
    class Authenticate {

        @Nested
        @DisplayName("ID 토큰 검증이 성공한 경우")
        class WhenIdTokenDecodeSucceeds {

            @Test
            @DisplayName("Jwt 클레임으로 매핑한 SocialIdentity를 반환한다.")
            void shouldReturnSocialIdentity() {
                // given
                Jwt jwt =
                        Jwt.withTokenValue(ID_TOKEN)
                                .header("alg", "RS256")
                                .subject("google123")
                                .claim("name", "구글 멤버")
                                .claim("email", "symphonia@google.com")
                                .claim("picture", "https://image.symphonia.com/profile/google")
                                .build();
                given(googleIdTokenDecoder.decode(ID_TOKEN)).willReturn(jwt);

                // when
                SocialIdentity identity = googleSocialClient.authenticate(ID_TOKEN);

                // then
                assertThat(identity.socialId()).isEqualTo("google123");
                assertThat(identity.nickname()).isEqualTo("구글 멤버");
                assertThat(identity.email()).isEqualTo("symphonia@google.com");
                assertThat(identity.profileImage())
                        .isEqualTo("https://image.symphonia.com/profile/google");
                assertThat(identity.socialProvider()).isEqualTo("GOOGLE");
            }
        }

        @Nested
        @DisplayName("서명, 발급자, aud, 만료 검증에 실패한 토큰인 경우")
        class WhenIdTokenIsInvalid {

            @Test
            @DisplayName("InvalidSocialTokenException이 발생한다.")
            void shouldThrowInvalidSocialTokenException() {
                // given
                given(googleIdTokenDecoder.decode(ID_TOKEN))
                        .willThrow(new BadJwtException("invalid id token"));

                // when & then
                assertThatThrownBy(() -> googleSocialClient.authenticate(ID_TOKEN))
                        .isInstanceOf(InvalidSocialTokenException.class)
                        .hasMessage(AuthErrorCode.INVALID_SOCIAL_TOKEN.getMessage());
            }
        }

        @Nested
        @DisplayName("공개키 목록을 가져오지 못한 경우")
        class WhenJwkSetRetrievalFails {

            @Test
            @DisplayName("SocialTokenVerificationUnavailableException이 발생한다.")
            void shouldThrowSocialTokenVerificationUnavailableException() {
                // given
                given(googleIdTokenDecoder.decode(ID_TOKEN))
                        .willThrow(new JwtException("Couldn't retrieve remote JWK set"));

                // when & then
                assertThatThrownBy(() -> googleSocialClient.authenticate(ID_TOKEN))
                        .isInstanceOf(SocialTokenVerificationUnavailableException.class)
                        .hasMessage(
                                AuthErrorCode.OAUTH_TOKEN_VERIFICATION_UNAVAILABLE.getMessage());
            }
        }
    }
}
