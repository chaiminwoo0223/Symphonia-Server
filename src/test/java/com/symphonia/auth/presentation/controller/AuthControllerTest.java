package com.symphonia.auth.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.domain.client.SocialClient;
import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.auth.fixture.SocialIdentityFixture;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.auth.presentation.cookie.CookieProvider;
import com.symphonia.auth.presentation.dto.request.LoginRequest;
import com.symphonia.auth.presentation.dto.request.SignupRequest;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.SocialProvider;
import com.symphonia.member.domain.repository.MemberRepository;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AuthControllerTest extends IntegrationTest {

    private static final String COOKIE_NAME = CookieProvider.COOKIE_NAME;
    private static final String AUTH_CODE = "auth-code";

    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthHelper authHelper;
    @Autowired private BlacklistAccessTokenRepository blacklistAccessTokenRepository;
    @Autowired private MemberRepository memberRepository;

    @MockitoBean(name = "kakao")
    private SocialClient kakaoSocialClient;

    @MockitoBean(name = "google")
    private SocialClient googleSocialClient;

    @Nested
    @DisplayName("POST /api/v1/auth/signup은")
    class Signup {

        @Nested
        @DisplayName("지원하는 provider의 유효한 인가 코드인 경우")
        class WhenAuthorizationCodeIsValid {

            @Test
            @DisplayName("멤버를 생성하고 201과 함께 토큰을 반환한다")
            void shouldCreateMemberAndReturnToken() throws Exception {
                // given
                given(kakaoSocialClient.authenticate(AUTH_CODE))
                        .willReturn(SocialIdentityFixture.KAKAO.create());
                SignupRequest request = new SignupRequest("kakao", AUTH_CODE);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.accessToken").exists())
                        .andExpect(cookie().exists(COOKIE_NAME));
                assertThat(
                                memberRepository.existsBySocialLogin(
                                        SocialProvider.KAKAO,
                                        SocialIdentityFixture.KAKAO.getSocialId()))
                        .isTrue();
            }
        }

        @Nested
        @DisplayName("이미 가입된 소셜 계정인 경우")
        class WhenSocialAccountAlreadyRegistered {

            @Test
            @DisplayName("409를 반환한다")
            void shouldReturnConflict() throws Exception {
                // given
                memberHelper.save(MemberFixture.KAKAO);
                given(kakaoSocialClient.authenticate(AUTH_CODE))
                        .willReturn(SocialIdentityFixture.KAKAO.create());
                SignupRequest request = new SignupRequest("kakao", AUTH_CODE);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isConflict());
            }
        }

        @Nested
        @DisplayName("지원하지 않는 provider인 경우")
        class WhenProviderUnsupported {

            @Test
            @DisplayName("400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                SignupRequest request = new SignupRequest("naver", AUTH_CODE);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("provider 또는 code가 빈 문자열인 경우")
        class WhenRequestFieldBlank {

            @Test
            @DisplayName("provider가 비어 있으면 400을 반환한다")
            void shouldReturnBadRequestWhenProviderBlank() throws Exception {
                // given
                SignupRequest request = new SignupRequest("", AUTH_CODE);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("code가 비어 있으면 400을 반환한다")
            void shouldReturnBadRequestWhenCodeBlank() throws Exception {
                // given
                SignupRequest request = new SignupRequest("kakao", "");

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("provider 또는 code가 길이 상한을 초과한 경우")
        class WhenRequestFieldTooLong {

            @Test
            @DisplayName("provider가 20자를 초과하면 400을 반환한다")
            void shouldReturnBadRequestWhenProviderTooLong() throws Exception {
                // given
                SignupRequest request = new SignupRequest("a".repeat(21), AUTH_CODE);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("code가 1000자를 초과하면 400을 반환한다")
            void shouldReturnBadRequestWhenCodeTooLong() throws Exception {
                // given
                SignupRequest request = new SignupRequest("kakao", "a".repeat(1001));

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login은")
    class Login {

        @Nested
        @DisplayName("가입된 소셜 계정인 경우")
        class WhenMemberRegistered {

            @Test
            @DisplayName("200과 함께 토큰을 반환한다")
            void shouldReturnToken() throws Exception {
                // given
                memberHelper.save(MemberFixture.KAKAO);
                given(kakaoSocialClient.authenticate(AUTH_CODE))
                        .willReturn(SocialIdentityFixture.KAKAO.create());
                LoginRequest request = new LoginRequest("kakao", AUTH_CODE);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.accessToken").exists())
                        .andExpect(cookie().exists(COOKIE_NAME));
            }
        }

        @Nested
        @DisplayName("가입되지 않은 소셜 계정인 경우")
        class WhenMemberNotRegistered {

            @Test
            @DisplayName("404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // given
                given(kakaoSocialClient.authenticate(AUTH_CODE))
                        .willReturn(SocialIdentityFixture.KAKAO.create());
                LoginRequest request = new LoginRequest("kakao", AUTH_CODE);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isNotFound());
            }
        }

        @Nested
        @DisplayName("지원하지 않는 provider인 경우")
        class WhenProviderUnsupported {

            @Test
            @DisplayName("400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                LoginRequest request = new LoginRequest("naver", AUTH_CODE);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("provider 또는 code가 빈 문자열인 경우")
        class WhenRequestFieldBlank {

            @Test
            @DisplayName("provider가 비어 있으면 400을 반환한다")
            void shouldReturnBadRequestWhenProviderBlank() throws Exception {
                // given
                LoginRequest request = new LoginRequest("", AUTH_CODE);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("code가 비어 있으면 400을 반환한다")
            void shouldReturnBadRequestWhenCodeBlank() throws Exception {
                // given
                LoginRequest request = new LoginRequest("kakao", "");

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("provider 또는 code가 길이 상한을 초과한 경우")
        class WhenRequestFieldTooLong {

            @Test
            @DisplayName("provider가 20자를 초과하면 400을 반환한다")
            void shouldReturnBadRequestWhenProviderTooLong() throws Exception {
                // given
                LoginRequest request = new LoginRequest("a".repeat(21), AUTH_CODE);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("code가 1000자를 초과하면 400을 반환한다")
            void shouldReturnBadRequestWhenCodeTooLong() throws Exception {
                // given
                LoginRequest request = new LoginRequest("kakao", "a".repeat(1001));

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh는")
    class Refresh {

        @Nested
        @DisplayName("유효한 리프레시 토큰 쿠키인 경우")
        class WhenRefreshTokenExists {

            @Test
            @DisplayName("새로운 액세스 토큰을 반환하고 리프레시 토큰 쿠키를 갱신한다")
            void shouldReturnAccessTokenAndRotateCookie() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String refreshToken = authHelper.issueRefreshTokenFor(member);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .cookie(new Cookie(COOKIE_NAME, refreshToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.accessToken").exists())
                        .andExpect(cookie().exists(COOKIE_NAME))
                        .andExpect(cookie().httpOnly(COOKIE_NAME, true))
                        .andExpect(cookie().path(COOKIE_NAME, "/api/v1/auth/refresh"));
            }
        }

        @Nested
        @DisplayName("리프레시 토큰 쿠키가 없는 경우")
        class WhenCookieMissing {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // when & then
                mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
            }
        }

        @Nested
        @DisplayName("쿠키 값이 저장된 토큰과 일치하지 않는 경우")
        class WhenCookieValueUnknown {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .cookie(new Cookie(COOKIE_NAME, "unknown-refresh-token")))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout는")
    class Logout {

        @Test
        @DisplayName("액세스 토큰을 블랙리스트에 등록하고 리프레시 토큰 쿠키를 만료시킨다")
        void shouldBlacklistAccessTokenAndExpireCookie() throws Exception {
            // given
            Member member = memberHelper.save(MemberFixture.KAKAO);
            String accessToken =
                    authHelper.generateAccessToken(
                            String.valueOf(member.getId()), member.getRole().name());

            // when & then
            mockMvc.perform(
                            post("/api/v1/auth/logout")
                                    .header(
                                            HttpHeaders.AUTHORIZATION,
                                            authHelper.bearerHeader(accessToken)))
                    .andExpect(status().isNoContent())
                    .andExpect(cookie().maxAge(COOKIE_NAME, 0));
            assertThat(blacklistAccessTokenRepository.isBlacklisted(accessToken)).isTrue();
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // when & then
                mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isUnauthorized());
            }
        }
    }
}
