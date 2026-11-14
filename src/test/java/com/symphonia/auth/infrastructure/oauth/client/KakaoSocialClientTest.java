package com.symphonia.auth.infrastructure.oauth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.symphonia.UnitTest;
import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.auth.domain.exception.InvalidSocialTokenException;
import com.symphonia.auth.domain.exception.SocialMemberInfoFetchFailedException;
import com.symphonia.auth.domain.exception.SocialRequiredInfoMissingException;
import com.symphonia.auth.domain.exception.SocialTokenVerificationFailedException;
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
import org.springframework.web.client.RestClient;

@DisplayName("KakaoSocialClient 단위 테스트")
class KakaoSocialClientTest extends UnitTest {

    private static final String ACCESS_TOKEN_INFO_URI =
            "https://kapi.kakao.com/v1/user/access_token_info";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";
    private static final String APP_ID = "1234";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String BEARER_ACCESS_TOKEN = "Bearer " + ACCESS_TOKEN;

    private MockRestServiceServer mockServer;
    private KakaoSocialClient kakaoSocialClient;

    @BeforeEach
    void setUp() {
        KakaoOAuthProperties properties =
                new KakaoOAuthProperties(APP_ID, ACCESS_TOKEN_INFO_URI, USER_INFO_URI);
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        kakaoSocialClient = new KakaoSocialClient(builder.build(), properties);
    }

    @Nested
    @DisplayName("authenticate 메서드는")
    class Authenticate {

        @Nested
        @DisplayName("토큰이 이 앱에서 발급됐고 사용자 정보 조회가 성공한 경우")
        class WhenTokenIssuedForThisAppAndUserInfoFetchSucceeds {

            @Test
            @DisplayName("SocialIdentity를 반환한다.")
            void shouldReturnSocialIdentity() {
                // given
                givenAccessTokenInfo(APP_ID);
                mockServer
                        .expect(requestTo(USER_INFO_URI))
                        .andExpect(method(HttpMethod.GET))
                        .andExpect(header(HttpHeaders.AUTHORIZATION, BEARER_ACCESS_TOKEN))
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
                SocialIdentity identity = kakaoSocialClient.authenticate(ACCESS_TOKEN);

                // then
                assertThat(identity.socialId()).isEqualTo("123456");
                assertThat(identity.nickname()).isEqualTo("카카오 멤버");
                assertThat(identity.email()).isEqualTo("symphonia@kakao.com");
                assertThat(identity.profileImage())
                        .isEqualTo("https://image.symphonia.com/profile/kakao");
                assertThat(identity.socialProvider()).isEqualTo("KAKAO");
                mockServer.verify();
            }
        }

        @Nested
        @DisplayName("다른 앱에서 발급된 토큰인 경우")
        class WhenTokenIssuedForOtherApp {

            // 사용자 정보 조회 요청을 기대하지 않으므로, 앱 ID가 다르면 조회하지 않는 것도 함께 검증한다.
            @Test
            @DisplayName("InvalidSocialTokenException이 발생한다.")
            void shouldThrowInvalidSocialTokenException() {
                // given
                givenAccessTokenInfo("9999");

                // when & then
                assertThatThrownBy(() -> kakaoSocialClient.authenticate(ACCESS_TOKEN))
                        .isInstanceOf(InvalidSocialTokenException.class)
                        .hasMessage(AuthErrorCode.INVALID_SOCIAL_TOKEN.getMessage());
                mockServer.verify();
            }
        }

        @Nested
        @DisplayName("토큰 정보 조회에 실패한 경우")
        class WhenAccessTokenInfoFetchFails {

            @Test
            @DisplayName("만료된 토큰이라 401 응답이면 InvalidSocialTokenException이 발생한다.")
            void shouldThrowInvalidSocialTokenExceptionWhenUnauthorized() {
                // given
                mockServer
                        .expect(requestTo(ACCESS_TOKEN_INFO_URI))
                        .andRespond(withUnauthorizedRequest());

                // when & then
                assertThatThrownBy(() -> kakaoSocialClient.authenticate(ACCESS_TOKEN))
                        .isInstanceOf(InvalidSocialTokenException.class)
                        .hasMessage(AuthErrorCode.INVALID_SOCIAL_TOKEN.getMessage());
            }

            @Test
            @DisplayName("형식이 잘못된 토큰이라 400 응답이면 InvalidSocialTokenException이 발생한다.")
            void shouldThrowInvalidSocialTokenExceptionWhenBadRequest() {
                // given
                mockServer.expect(requestTo(ACCESS_TOKEN_INFO_URI)).andRespond(withBadRequest());

                // when & then
                assertThatThrownBy(() -> kakaoSocialClient.authenticate(ACCESS_TOKEN))
                        .isInstanceOf(InvalidSocialTokenException.class)
                        .hasMessage(AuthErrorCode.INVALID_SOCIAL_TOKEN.getMessage());
            }

            @Test
            @DisplayName("서버 오류 응답이면 SocialTokenVerificationFailedException이 발생한다.")
            void shouldThrowSocialTokenVerificationFailedExceptionWhenServerError() {
                // given
                mockServer.expect(requestTo(ACCESS_TOKEN_INFO_URI)).andRespond(withServerError());

                // when & then
                assertThatThrownBy(() -> kakaoSocialClient.authenticate(ACCESS_TOKEN))
                        .isInstanceOf(SocialTokenVerificationFailedException.class)
                        .hasMessage(AuthErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED.getMessage());
            }

            @Test
            @DisplayName("app_id가 없는 응답이면 SocialTokenVerificationFailedException이 발생한다.")
            void shouldThrowSocialTokenVerificationFailedExceptionWhenAppIdMissing() {
                // given
                mockServer
                        .expect(requestTo(ACCESS_TOKEN_INFO_URI))
                        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

                // when & then
                assertThatThrownBy(() -> kakaoSocialClient.authenticate(ACCESS_TOKEN))
                        .isInstanceOf(SocialTokenVerificationFailedException.class)
                        .hasMessage(AuthErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED.getMessage());
            }
        }

        @Nested
        @DisplayName("이메일이나 닉네임이 제공되지 않은 경우")
        class WhenRequiredInfoMissing {

            @BeforeEach
            void setUp() {
                givenAccessTokenInfo(APP_ID);
            }

            @Test
            @DisplayName("email이 없으면 SocialRequiredInfoMissingException이 발생한다.")
            void shouldThrowSocialRequiredInfoMissingExceptionWhenEmailMissing() {
                // given
                mockServer
                        .expect(requestTo(USER_INFO_URI))
                        .andRespond(
                                withSuccess(
                                        """
                                        {
                                          "id": 123456,
                                          "kakao_account": {
                                            "profile": {
                                              "nickname": "카카오 멤버"
                                            }
                                          }
                                        }
                                        """,
                                        MediaType.APPLICATION_JSON));

                // when & then
                assertThatThrownBy(() -> kakaoSocialClient.authenticate(ACCESS_TOKEN))
                        .isInstanceOf(SocialRequiredInfoMissingException.class)
                        .hasMessage(AuthErrorCode.SOCIAL_REQUIRED_INFO_MISSING.getMessage());
            }

            @Test
            @DisplayName("nickname이 없으면 SocialRequiredInfoMissingException이 발생한다.")
            void shouldThrowSocialRequiredInfoMissingExceptionWhenNicknameMissing() {
                // given
                mockServer
                        .expect(requestTo(USER_INFO_URI))
                        .andRespond(
                                withSuccess(
                                        """
                                        {
                                          "id": 123456,
                                          "kakao_account": {
                                            "email": "symphonia@kakao.com"
                                          }
                                        }
                                        """,
                                        MediaType.APPLICATION_JSON));

                // when & then
                assertThatThrownBy(() -> kakaoSocialClient.authenticate(ACCESS_TOKEN))
                        .isInstanceOf(SocialRequiredInfoMissingException.class)
                        .hasMessage(AuthErrorCode.SOCIAL_REQUIRED_INFO_MISSING.getMessage());
            }

            @Test
            @DisplayName("kakao_account가 없으면 SocialRequiredInfoMissingException이 발생한다.")
            void shouldThrowSocialRequiredInfoMissingExceptionWhenKakaoAccountMissing() {
                // given
                mockServer
                        .expect(requestTo(USER_INFO_URI))
                        .andRespond(withSuccess("{\"id\": 123456}", MediaType.APPLICATION_JSON));

                // when & then
                assertThatThrownBy(() -> kakaoSocialClient.authenticate(ACCESS_TOKEN))
                        .isInstanceOf(SocialRequiredInfoMissingException.class)
                        .hasMessage(AuthErrorCode.SOCIAL_REQUIRED_INFO_MISSING.getMessage());
            }
        }

        @Nested
        @DisplayName("사용자 정보 조회에 실패한 경우")
        class WhenUserInfoFetchFails {

            @BeforeEach
            void setUp() {
                givenAccessTokenInfo(APP_ID);
            }

            @Test
            @DisplayName("서버 오류 응답이면 SocialMemberInfoFetchFailedException이 발생한다.")
            void shouldThrowSocialMemberInfoFetchFailedException() {
                // given
                mockServer.expect(requestTo(USER_INFO_URI)).andRespond(withServerError());

                // when & then
                assertThatThrownBy(() -> kakaoSocialClient.authenticate(ACCESS_TOKEN))
                        .isInstanceOf(SocialMemberInfoFetchFailedException.class)
                        .hasMessage(AuthErrorCode.OAUTH_MEMBER_INFO_FETCH_FAILED.getMessage());
            }
        }
    }

    private void givenAccessTokenInfo(String appId) {
        mockServer
                .expect(requestTo(ACCESS_TOKEN_INFO_URI))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, BEARER_ACCESS_TOKEN))
                .andRespond(
                        withSuccess(
                                "{\"id\": 123456, \"expires_in\": 7199, \"app_id\": " + appId + "}",
                                MediaType.APPLICATION_JSON));
    }
}
