package com.symphonia.gathering.presentation.controller;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.error.GatheringErrorCode;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.gathering.presentation.dto.request.OpenGatheringRequest;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class GatheringControllerTest extends IntegrationTest {

    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthHelper authHelper;

    @Nested
    @DisplayName("POST /api/v1/gatherings는")
    class Open {

        @Nested
        @DisplayName("인증된 요청인 경우")
        class WhenAuthenticated {

            @Test
            @DisplayName("모임을 열고 참여 토큰과 함께 201을 반환한다")
            void shouldOpenGathering() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                OpenGatheringRequest request =
                        new OpenGatheringRequest(GatheringFixture.FRIDAY_DINNER.getName());

                // when & then
                mockMvc.perform(
                                post("/api/v1/gatherings")
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.gatheringId").isNumber())
                        .andExpect(jsonPath("$.data.joinToken").isNotEmpty());
            }
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // given
                OpenGatheringRequest request =
                        new OpenGatheringRequest(GatheringFixture.FRIDAY_DINNER.getName());

                // when & then
                mockMvc.perform(
                                post("/api/v1/gatherings")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gatherings/{gatheringId}/join-token은")
    class Reissue {

        @Nested
        @DisplayName("모임을 만든 회원의 요청인 경우")
        class WhenMemberIsHost {

            @Test
            @DisplayName("새 참여 토큰과 함께 201을 반환한다")
            void shouldReissueJoinToken() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());
                String oldJoinToken = gatheringHelper.issueJoinToken(gathering);

                // when & then
                mockMvc.perform(
                                post(
                                                "/api/v1/gatherings/{gatheringId}/join-token",
                                                gathering.getId())
                                        .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.gatheringId").value(gathering.getId()))
                        .andExpect(jsonPath("$.data.joinToken").isNotEmpty())
                        .andExpect(jsonPath("$.data.joinToken").value(not(oldJoinToken)));
            }
        }

        @Nested
        @DisplayName("모임을 만들지 않은 다른 회원의 요청인 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("NOT_GATHERING_HOST 코드와 함께 403을 반환한다")
            void shouldReturnForbidden() throws Exception {
                // given
                Member host = memberHelper.save(MemberFixture.KAKAO);
                Member otherMember = memberHelper.save(MemberFixture.GOOGLE);
                String token = authHelper.bearerTokenFor(otherMember);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, host.getId());

                // when & then
                mockMvc.perform(
                                post(
                                                "/api/v1/gatherings/{gatheringId}/join-token",
                                                gathering.getId())
                                        .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_GATHERING_HOST.getCode()));
            }
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

                // when & then
                mockMvc.perform(
                                post(
                                        "/api/v1/gatherings/{gatheringId}/join-token",
                                        gathering.getId()))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/gatherings/preview는")
    class Preview {

        @Test
        @DisplayName("인증 없이도 모임 이름, 참여 가능 여부, 동의 안내 버전을 반환한다")
        void shouldReturnGatheringWithoutAuthentication() throws Exception {
            // given
            String joinToken = saveGatheringWithJoinToken();

            // when & then
            mockMvc.perform(get("/api/v1/gatherings/preview").header("X-Join-Token", joinToken))
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.data.name").value(GatheringFixture.FRIDAY_DINNER.getName()))
                    .andExpect(jsonPath("$.data.joinable").value(true))
                    .andExpect(
                            jsonPath("$.data.consentVersion")
                                    .value(GatheringPolicy.CONSENT_VERSION));
        }
    }

    private String saveGatheringWithJoinToken() {
        Member member = memberHelper.save(MemberFixture.KAKAO);
        Gathering gathering = gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

        return gatheringHelper.issueJoinToken(gathering);
    }
}
