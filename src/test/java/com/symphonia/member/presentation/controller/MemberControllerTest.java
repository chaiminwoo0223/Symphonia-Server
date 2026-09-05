package com.symphonia.member.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.helper.AuthTestHelper;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import com.symphonia.member.presentation.dto.request.MemberUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class MemberControllerTest extends IntegrationTest {

    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthTestHelper authTestHelper;

    @Nested
    @DisplayName("GET /api/v1/members/me는")
    class Get {

        @Nested
        @DisplayName("인증된 요청인 경우")
        class WhenAuthenticated {

            @Test
            @DisplayName("멤버 정보를 반환한다")
            void shouldReturnMember() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = generateBearerToken(member);

                // when & then
                mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.id").value(member.getId()))
                        .andExpect(jsonPath("$.data.nickname").value(member.getNickname()));
            }
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // when & then
                mockMvc.perform(get("/api/v1/members/me")).andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/members/me는")
    class Update {

        @Test
        @DisplayName("닉네임을 수정한다")
        void shouldUpdateNickname() throws Exception {
            // given
            Member member = memberHelper.save(MemberFixture.KAKAO);
            String token = generateBearerToken(member);
            MemberUpdateRequest request = new MemberUpdateRequest("새로운 닉네임");

            // when & then
            mockMvc.perform(
                            patch("/api/v1/members/me")
                                    .header(HttpHeaders.AUTHORIZATION, token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value("새로운 닉네임"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/members/me는")
    class Delete {

        @Test
        @DisplayName("멤버를 삭제한다")
        void shouldDeleteMember() throws Exception {
            // given
            Member member = memberHelper.save(MemberFixture.KAKAO);
            String token = generateBearerToken(member);

            // when & then
            mockMvc.perform(delete("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isNoContent());
        }
    }

    private String generateBearerToken(Member member) {
        String accessToken =
                authTestHelper.generateAccessToken(
                        String.valueOf(member.getId()), member.getRole().name());

        return authTestHelper.bearerHeader(accessToken);
    }
}
