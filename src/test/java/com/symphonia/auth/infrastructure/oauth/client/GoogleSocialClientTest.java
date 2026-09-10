package com.symphonia.auth.infrastructure.oauth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.symphonia.UnitTest;
import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.auth.domain.exception.SocialAuthenticationFailedException;
import com.symphonia.auth.domain.exception.SocialMemberInfoFetchFailedException;
import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.auth.infrastructure.oauth.config.properties.GoogleOAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("GoogleSocialClient 단위 테스트")
class GoogleSocialClientTest extends UnitTest {

    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String CODE = "auth-code";
    private static final String ID_TOKEN = "id-token-value";

    @Mock private JwtDecoder googleIdTokenDecoder;

    private MockRestServiceServer mockServer;
    private GoogleSocialClient googleSocialClient;

    @BeforeEach
    void setUp() {
        GoogleOAuthProperties properties =
                new GoogleOAuthProperties(
                        "client-id",
                        "client-secret",
                        "redirect-uri",
                        TOKEN_URI,
                        "jwks-uri",
                        "issuer");
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        googleSocialClient =
                new GoogleSocialClient(builder.build(), googleIdTokenDecoder, properties);
    }

    @Nested
    @DisplayName("authenticate 메서드는")
    class Authenticate {

        @Nested
        @DisplayName("토큰 교환과 ID 토큰 검증이 모두 성공한 경우")
        class WhenTokenExchangeAndDecodeSucceed {

            @Test
            @DisplayName("Jwt 클레임으로 매핑한 SocialIdentity를 반환한다.")
            void shouldReturnSocialIdentity() {
                // given
                mockServer
                        .expect(requestTo(TOKEN_URI))
                        .andExpect(method(HttpMethod.POST))
                        .andRespond(
                                withSuccess(
                                        "{\"id_token\": \"" + ID_TOKEN + "\"}",
                                        MediaType.APPLICATION_JSON));
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
                SocialIdentity identity = googleSocialClient.authenticate(CODE);

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
        @DisplayName("토큰 교환에 실패한 경우")
        class WhenTokenExchangeFails {

            // 서버 오류·인가 코드 무효 분기는 Kakao/Google이 공유하는 AuthorizationCodeClient의 책임이라
            // AuthorizationCodeClientTest에서 검증한다.
            // 여기서는 이 SocialClient만의 관심사인 "id_token 누락" 분기만 확인한다.
            @Test
            @DisplayName("id_token이 없는 응답이면 예외가 발생한다.")
            void shouldThrowExceptionWhenIdTokenMissing() {
                // given
                mockServer
                        .expect(requestTo(TOKEN_URI))
                        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

                // when & then
                assertThatThrownBy(() -> googleSocialClient.authenticate(CODE))
                        .isInstanceOf(SocialAuthenticationFailedException.class)
                        .hasMessage(AuthErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED.getMessage());
            }
        }

        @Nested
        @DisplayName("ID 토큰 검증에 실패한 경우")
        class WhenIdTokenDecodeFails {

            @BeforeEach
            void setUp() {
                mockServer
                        .expect(requestTo(TOKEN_URI))
                        .andRespond(
                                withSuccess(
                                        "{\"id_token\": \"" + ID_TOKEN + "\"}",
                                        MediaType.APPLICATION_JSON));
            }

            @Test
            @DisplayName("예외가 발생한다.")
            void shouldThrowException() {
                // given
                given(googleIdTokenDecoder.decode(ID_TOKEN))
                        .willThrow(new BadJwtException("invalid id token"));

                // when & then
                assertThatThrownBy(() -> googleSocialClient.authenticate(CODE))
                        .isInstanceOf(SocialMemberInfoFetchFailedException.class)
                        .hasMessage(AuthErrorCode.OAUTH_MEMBER_INFO_FETCH_FAILED.getMessage());
            }
        }
    }
}
