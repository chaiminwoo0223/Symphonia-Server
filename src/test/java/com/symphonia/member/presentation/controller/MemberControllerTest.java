package com.symphonia.member.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.common.exception.error.CommonErrorCode;
import com.symphonia.member.application.event.MemberDeletedEvent;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import com.symphonia.member.presentation.dto.request.UpdateMemberRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.transaction.TestTransaction;

@RecordApplicationEvents
class MemberControllerTest extends IntegrationTest {

    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthHelper authHelper;
    @Autowired private ApplicationEvents applicationEvents;
    @Autowired private BlacklistAccessTokenRepository blacklistAccessTokenRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

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
                String token = authHelper.bearerTokenFor(member);

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
            String token = authHelper.bearerTokenFor(member);
            UpdateMemberRequest request = new UpdateMemberRequest("새로운 닉네임");

            // when & then
            mockMvc.perform(
                            patch("/api/v1/members/me")
                                    .header(HttpHeaders.AUTHORIZATION, token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value("새로운 닉네임"));
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // given
                UpdateMemberRequest request = new UpdateMemberRequest("새로운 닉네임");

                // when & then
                mockMvc.perform(
                                patch("/api/v1/members/me")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
            }
        }

        @Nested
        @DisplayName("닉네임이 빈 값인 경우")
        class WhenNicknameBlank {

            @Test
            @DisplayName("400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                UpdateMemberRequest request = new UpdateMemberRequest("");

                // when & then
                mockMvc.perform(
                                patch("/api/v1/members/me")
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.ok").value(false))
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("nickname"));
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/members/me는")
    class Delete {

        @Test
        @DisplayName("멤버를 삭제하고 MemberDeletedEvent를 발행한다")
        void shouldDeleteMemberAndPublishMemberDeletedEvent() throws Exception {
            // given
            Member member = memberHelper.save(MemberFixture.KAKAO);
            String accessToken =
                    authHelper.generateAccessToken(
                            String.valueOf(member.getId()), member.getRole().name());

            // when & then
            mockMvc.perform(
                            delete("/api/v1/members/me")
                                    .header(
                                            HttpHeaders.AUTHORIZATION,
                                            authHelper.bearerHeader(accessToken)))
                    .andExpect(status().isNoContent());
            assertThat(applicationEvents.stream(MemberDeletedEvent.class))
                    .anySatisfy(
                            event -> {
                                assertThat(event.memberId()).isEqualTo(member.getId());
                                assertThat(event.accessToken()).isEqualTo(accessToken);
                            });
        }

        @Test
        @DisplayName("멤버 삭제가 커밋되면 액세스 토큰을 블랙리스트에 등록하고 리프레시 토큰을 삭제한다")
        void shouldBlacklistAccessTokenAndDeleteRefreshTokenWhenCommitted() throws Exception {
            // given
            Member member = memberHelper.save(MemberFixture.KAKAO);
            String accessToken =
                    authHelper.generateAccessToken(
                            String.valueOf(member.getId()), member.getRole().name());
            String refreshToken = authHelper.issueRefreshTokenFor(member);

            // when
            mockMvc.perform(
                            delete("/api/v1/members/me")
                                    .header(
                                            HttpHeaders.AUTHORIZATION,
                                            authHelper.bearerHeader(accessToken)))
                    .andExpect(status().isNoContent());
            TestTransaction.flagForCommit();
            TestTransaction.end();

            // then
            assertThat(blacklistAccessTokenRepository.isBlacklisted(accessToken)).isTrue();
            assertThat(refreshTokenRepository.findMemberIdByValue(refreshToken)).isEmpty();
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // when & then
                mockMvc.perform(delete("/api/v1/members/me")).andExpect(status().isUnauthorized());
            }
        }
    }
}
