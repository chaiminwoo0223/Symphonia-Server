package com.symphonia.global.security.filter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.auth.domain.repository.WithdrawnMemberRepository;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class JwtAuthenticationFilterTest extends IntegrationTest {

    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthHelper authHelper;

    @MockitoBean private BlacklistAccessTokenRepository blacklistAccessTokenRepository;
    @MockitoBean private WithdrawnMemberRepository withdrawnMemberRepository;

    @Nested
    @DisplayName("토큰 저장소 조회가 실패하면")
    class WhenTokenStoreFails {

        @Nested
        @DisplayName("공개 엔드포인트는")
        class PublicEndpoint {

            @Test
            @DisplayName("블랙리스트 조회가 실패해도 비회원으로 처리해 200을 반환한다")
            void shouldReturnOkWhenBlacklistLookupFails() throws Exception {
                // given
                String bearer = bearerTokenOfMember();
                given(blacklistAccessTokenRepository.isBlacklisted(anyString()))
                        .willThrow(new QueryTimeoutException("redis down"));

                // when & then
                mockMvc.perform(
                                get("/api/v1/pairings/recommendations")
                                        .header(HttpHeaders.AUTHORIZATION, bearer)
                                        .param("relationshipType", "FRIEND")
                                        .param("moodType", "CASUAL")
                                        .param("isAdultConfirmed", "true"))
                        .andExpect(status().isOk());
            }

            @Test
            @DisplayName("탈퇴 회원 조회가 실패해도 비회원으로 처리해 200을 반환한다")
            void shouldReturnOkWhenWithdrawnLookupFails() throws Exception {
                // given
                String bearer = bearerTokenOfMember();
                given(withdrawnMemberRepository.isWithdrawn(anyString()))
                        .willThrow(new QueryTimeoutException("redis down"));

                // when & then
                mockMvc.perform(
                                get("/api/v1/pairings/recommendations")
                                        .header(HttpHeaders.AUTHORIZATION, bearer)
                                        .param("relationshipType", "FRIEND")
                                        .param("moodType", "CASUAL")
                                        .param("isAdultConfirmed", "true"))
                        .andExpect(status().isOk());
            }
        }

        @Nested
        @DisplayName("인증이 필요한 엔드포인트는")
        class ProtectedEndpoint {

            @Test
            @DisplayName("401이 아니라 503과 TOKEN_STORE_UNAVAILABLE을 반환한다")
            void shouldReturnServiceUnavailable() throws Exception {
                // given
                String bearer = bearerTokenOfMember();
                given(blacklistAccessTokenRepository.isBlacklisted(anyString()))
                        .willThrow(new QueryTimeoutException("redis down"));

                // when & then
                mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, bearer))
                        .andExpect(status().isServiceUnavailable())
                        .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
                        .andExpect(
                                jsonPath("$.code")
                                        .value(AuthErrorCode.TOKEN_STORE_UNAVAILABLE.getCode()));
            }
        }
    }

    private String bearerTokenOfMember() {
        Member member = memberHelper.save(MemberFixture.KAKAO);

        return authHelper.bearerTokenFor(member);
    }
}
