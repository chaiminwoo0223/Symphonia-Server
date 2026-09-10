package com.symphonia.auth.infrastructure.oauth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.symphonia.UnitTest;
import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.auth.domain.exception.SocialAuthenticationFailedException;
import com.symphonia.auth.domain.exception.SocialMemberInfoFetchFailedException;
import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.auth.infrastructure.oauth.config.properties.KakaoOAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@DisplayName("KakaoSocialClient 단위 테스트")
class KakaoSocialClientTest extends UnitTest {

    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";
    private static final String CODE = "auth-code";

    private MockRestServiceServer mockServer;
    private KakaoSocialClient kakaoSocialClient;

    @BeforeEach
    void setUp() {
        KakaoOAuthProperties properties =
                new KakaoOAuthProperties(
                        "client-id", "client-secret", "redirect-uri", TOKEN_URI, USER_INFO_URI);
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        kakaoSocialClient = new KakaoSocialClient(builder.build(), properties);
    }

    @Nested
    @DisplayName("authenticate 메서드는")
    class Authenticate {

        @Nested
        @DisplayName("토큰 교환과 사용자 정보 조회가 모두 성공한 경우")
        class WhenTokenExchangeAndUserInfoFetchSucceed {

            @Test
            @DisplayName("SocialIdentity를 반환한다.")
            void shouldReturnSocialIdentity() {
                // given
                MultiValueMap<String, String> expectedTokenRequest = new LinkedMultiValueMap<>();
                expectedTokenRequest.add("grant_type", "authorization_code");
                expectedTokenRequest.add("client_id", "client-id");
                expectedTokenRequest.add("client_secret", "client-secret");
                expectedTokenRequest.add("redirect_uri", "redirect-uri");
                expectedTokenRequest.add("code", CODE);

                mockServer
                        .expect(requestTo(TOKEN_URI))
                        .andExpect(method(HttpMethod.POST))
                        .andExpect(content().formData(expectedTokenRequest))
                        .andRespond(
                                withSuccess(
                                        "{\"access_token\": \"access-token\"}",
                                        MediaType.APPLICATION_JSON));
                mockServer
                        .expect(requestTo(USER_INFO_URI))
                        .andExpect(method(HttpMethod.GET))
                        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                        .andRespond(
                                withSuccess(
                                        """
                                        {
                                          "id": 123456,
                                          "kakao_account": {
                                            "email": "symphonia@kakao.com",
                                            "profile": {
                                              "nickname": "카카오 멤버",
                                              "profile_image_url": "https://image.symphonia.com/profile/kakao"
                                            }
                                          }
                                        }
                                        """,
                                        MediaType.APPLICATION_JSON));

                // when
                SocialIdentity identity = kakaoSocialClient.authenticate(CODE);

                // then
                assertThat(identity.socialId()).isEqualTo("123456");
                assertThat(identity.nickname()).isEqualTo("카카오 멤버");
                assertThat(identity.email()).isEqualTo("symphonia@kakao.com");
                assertThat(identity.profileImage())
                        .isEqualTo("https://image.symphonia.com/profile/kakao");
                assertThat(identity.socialProvider()).isEqualTo("KAKAO");
                mockServer.verify();
            }

            @Test
            @DisplayName("kakao_account가 없으면 nickname, email, profileImage는 null이다.")
            void shouldReturnSocialIdentityWithNullFieldsWhenKakaoAccountMissing() {
                // given
                mockServer
                        .expect(requestTo(TOKEN_URI))
                        .andRespond(
                                withSuccess(
                                        "{\"access_token\": \"access-token\"}",
                                        MediaType.APPLICATION_JSON));
                mockServer
                        .expect(requestTo(USER_INFO_URI))
                        .andRespond(withSuccess("{\"id\": 123456}", MediaType.APPLICATION_JSON));

                // when
                SocialIdentity identity = kakaoSocialClient.authenticate(CODE);

                // then
                assertThat(identity.socialId()).isEqualTo("123456");
                assertThat(identity.nickname()).isNull();
                assertThat(identity.email()).isNull();
                assertThat(identity.profileImage()).isNull();
            }
        }

        @Nested
        @DisplayName("토큰 교환에 실패한 경우")
        class WhenTokenExchangeFails {

            // 서버 오류·인가 코드 무효 분기는 Kakao/Google이 공유하는 AuthorizationCodeClient의 책임이라
            // AuthorizationCodeClientTest에서 검증한다.
            // 여기서는 이 SocialClient만의 관심사인 "access_token 누락" 분기만 확인한다.
            @Test
            @DisplayName("access_token이 없는 응답이면 예외가 발생한다.")
            void shouldThrowExceptionWhenAccessTokenMissing() {
                // given
                mockServer
                        .expect(requestTo(TOKEN_URI))
                        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

                // when & then
                assertThatThrownBy(() -> kakaoSocialClient.authenticate(CODE))
                        .isInstanceOf(SocialAuthenticationFailedException.class)
                        .hasMessage(AuthErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED.getMessage());
            }
        }

        @Nested
        @DisplayName("사용자 정보 조회에 실패한 경우")
        class WhenUserInfoFetchFails {

            @BeforeEach
            void setUp() {
                mockServer
                        .expect(requestTo(TOKEN_URI))
                        .andRespond(
                                withSuccess(
                                        "{\"access_token\": \"access-token\"}",
                                        MediaType.APPLICATION_JSON));
            }

            @Test
            @DisplayName("서버 오류 응답이면 예외가 발생한다.")
            void shouldThrowException() {
                // given
                mockServer.expect(requestTo(USER_INFO_URI)).andRespond(withServerError());

                // when & then
                assertThatThrownBy(() -> kakaoSocialClient.authenticate(CODE))
                        .isInstanceOf(SocialMemberInfoFetchFailedException.class)
                        .hasMessage(AuthErrorCode.OAUTH_MEMBER_INFO_FETCH_FAILED.getMessage());
            }
        }
    }
}
