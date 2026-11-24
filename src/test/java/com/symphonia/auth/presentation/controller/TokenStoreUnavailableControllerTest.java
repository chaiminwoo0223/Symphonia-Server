package com.symphonia.auth.presentation.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.auth.infrastructure.redis.RefreshTokenRedisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class TokenStoreUnavailableControllerTest extends IntegrationTest {

    private static final String REFRESH_TOKEN_BODY = "{\"refreshToken\":\"refresh-token\"}";

    @MockitoBean private RefreshTokenRedisRepository refreshTokenRedisRepository;

    @Nested
    @DisplayName("POST /api/v1/auth/logout는")
    class Logout {

        @Nested
        @DisplayName("리프레시 토큰 저장소 조회가 실패한 경우")
        class WhenRefreshTokenStoreFails {

            @Test
            @DisplayName("500이 아니라 503과 TOKEN_STORE_UNAVAILABLE을 반환한다")
            void shouldReturnServiceUnavailable() throws Exception {
                // given
                given(refreshTokenRedisRepository.findById(anyString()))
                        .willThrow(new QueryTimeoutException("redis down"));

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/logout")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(REFRESH_TOKEN_BODY))
                        .andExpect(status().isServiceUnavailable())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(AuthErrorCode.TOKEN_STORE_UNAVAILABLE.getCode()));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh는")
    class Refresh {

        @Nested
        @DisplayName("리프레시 토큰 저장소 조회가 실패한 경우")
        class WhenRefreshTokenStoreFails {

            @Test
            @DisplayName("500이 아니라 503과 TOKEN_STORE_UNAVAILABLE을 반환한다")
            void shouldReturnServiceUnavailable() throws Exception {
                // given
                given(refreshTokenRedisRepository.findById(anyString()))
                        .willThrow(new QueryTimeoutException("redis down"));

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(REFRESH_TOKEN_BODY))
                        .andExpect(status().isServiceUnavailable())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(AuthErrorCode.TOKEN_STORE_UNAVAILABLE.getCode()));
            }
        }
    }
}
