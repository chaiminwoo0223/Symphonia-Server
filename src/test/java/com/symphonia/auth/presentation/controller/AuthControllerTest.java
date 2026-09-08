package com.symphonia.auth.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.auth.presentation.cookie.CookieProvider;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

class AuthControllerTest extends IntegrationTest {

    private static final String COOKIE_NAME = CookieProvider.COOKIE_NAME;
    private static final Long REFRESH_TOKEN_EXPIRATION_TIME = 3600L;

    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthHelper authHelper;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private BlacklistAccessTokenRepository blacklistAccessTokenRepository;

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
                String refreshTokenValue = "refresh-token-value";
                refreshTokenRepository.save(
                        refreshTokenValue,
                        String.valueOf(member.getId()),
                        REFRESH_TOKEN_EXPIRATION_TIME);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .cookie(new Cookie(COOKIE_NAME, refreshTokenValue)))
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
    }
}
