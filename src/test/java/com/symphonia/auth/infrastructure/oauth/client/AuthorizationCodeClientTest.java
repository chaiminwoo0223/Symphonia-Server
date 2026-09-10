package com.symphonia.auth.infrastructure.oauth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.auth.domain.exception.InvalidAuthorizationCodeException;
import com.symphonia.auth.domain.exception.SocialAuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

// KakaoSocialClient, GoogleSocialClient가 공통으로 위임하는 authorization_code 교환 로직을 여기서 한 번만 검증한다.
// 각 SocialClient 테스트는 이 클래스가 반환/전파하는 예외가 자신의 흐름에서 발생한다는 것만 확인하면 되고,
// 실패 분기(서버 오류, 인가 코드 무효)의 세부 동작까지 중복해서 검증하지 않는다.
@DisplayName("AuthorizationCodeClient 단위 테스트")
class AuthorizationCodeClientTest {

    private static final String TOKEN_URI = "https://provider.example.com/oauth/token";
    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_SECRET = "client-secret";
    private static final String REDIRECT_URI = "redirect-uri";
    private static final String CODE = "auth-code";

    private MockRestServiceServer mockServer;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    @Nested
    @DisplayName("exchange 메서드는")
    class Exchange {

        @Test
        @DisplayName("authorization_code 그랜트 타입의 폼 데이터로 토큰 엔드포인트를 호출한다.")
        void shouldRequestWithAuthorizationCodeGrantType() {
            // given
            MultiValueMap<String, String> expectedRequest = new LinkedMultiValueMap<>();
            expectedRequest.add("grant_type", "authorization_code");
            expectedRequest.add("client_id", CLIENT_ID);
            expectedRequest.add("client_secret", CLIENT_SECRET);
            expectedRequest.add("redirect_uri", REDIRECT_URI);
            expectedRequest.add("code", CODE);

            mockServer
                    .expect(requestTo(TOKEN_URI))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().formData(expectedRequest))
                    .andRespond(
                            withSuccess(
                                    "{\"access_token\": \"token\"}", MediaType.APPLICATION_JSON));

            // when
            String response = exchange();

            // then
            assertThat(response).contains("token");
            mockServer.verify();
        }

        @Test
        @DisplayName("서버 오류 응답이면 SocialAuthenticationFailedException이 발생한다.")
        void shouldThrowSocialAuthenticationFailedExceptionWhenServerError() {
            // given
            mockServer.expect(requestTo(TOKEN_URI)).andRespond(withServerError());

            // when & then
            assertThatThrownBy(this::exchange)
                    .isInstanceOf(SocialAuthenticationFailedException.class)
                    .hasMessage(AuthErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED.getMessage());
        }

        @Test
        @DisplayName("만료되었거나 이미 사용된 인가 코드(4xx)면 InvalidAuthorizationCodeException이 발생한다.")
        void shouldThrowInvalidAuthorizationCodeExceptionWhenBadRequest() {
            // given
            mockServer.expect(requestTo(TOKEN_URI)).andRespond(withBadRequest());

            // when & then
            assertThatThrownBy(this::exchange)
                    .isInstanceOf(InvalidAuthorizationCodeException.class)
                    .hasMessage(AuthErrorCode.INVALID_AUTHORIZATION_CODE.getMessage());
        }

        private String exchange() {
            return AuthorizationCodeClient.exchange(
                    restClient,
                    TOKEN_URI,
                    CLIENT_ID,
                    CLIENT_SECRET,
                    REDIRECT_URI,
                    CODE,
                    String.class);
        }
    }
}
