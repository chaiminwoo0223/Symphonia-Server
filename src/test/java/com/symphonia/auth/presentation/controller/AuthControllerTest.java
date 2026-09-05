package com.symphonia.auth.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.auth.presentation.dto.request.RefreshRequest;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AuthControllerTest extends IntegrationTest {

    private static final Long REFRESH_TOKEN_EXPIRATION_TIME = 3600L;

    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthHelper authHelper;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private BlacklistAccessTokenRepository blacklistAccessTokenRepository;

    @Nested
    @DisplayName("POST /api/v1/auth/refresh는")
    class Refresh {

        @Nested
        @DisplayName("유효한 리프레시 토큰인 경우")
        class WhenRefreshTokenExists {

            @Test
            @DisplayName("새로운 토큰 쌍을 반환한다")
            void shouldReturnNewTokenPair() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String refreshTokenValue = "refresh-token-value";
                refreshTokenRepository.save(
                        refreshTokenValue,
                        String.valueOf(member.getId()),
                        REFRESH_TOKEN_EXPIRATION_TIME);
                RefreshRequest request = new RefreshRequest(refreshTokenValue);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.accessToken").exists())
                        .andExpect(jsonPath("$.data.refreshToken").exists());
            }
        }

        @Nested
        @DisplayName("존재하지 않는 리프레시 토큰인 경우")
        class WhenRefreshTokenNotFound {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // given
                RefreshRequest request = new RefreshRequest("unknown-refresh-token");

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout는")
    class Logout {

        @Test
        @DisplayName("액세스 토큰을 블랙리스트에 등록하고 204를 반환한다")
        void shouldBlacklistAccessTokenAndReturnNoContent() throws Exception {
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
                    .andExpect(status().isNoContent());
            assertThat(blacklistAccessTokenRepository.isBlacklisted(accessToken)).isTrue();
        }
    }
}
