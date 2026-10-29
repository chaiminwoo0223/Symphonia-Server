package com.symphonia.gathering.presentation.controller;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.common.exception.error.CommonErrorCode;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.gathering.presentation.dto.request.CreateGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.JoinGatheringRequest;
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

    private static final String HYPHENATED_PHONE_NUMBER = "010-1234-5678";
    private static final String MASKED_PHONE_NUMBER = "010-****-5678";

    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthHelper authHelper;

    @Nested
    @DisplayName("POST /api/v1/gatherings는")
    class Create {

        @Nested
        @DisplayName("인증된 요청인 경우")
        class WhenAuthenticated {

            @Test
            @DisplayName("모임을 만들고 참여 토큰과 함께 201을 반환한다")
            void shouldCreateGathering() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                CreateGatheringRequest request =
                        new CreateGatheringRequest(GatheringFixture.FRIDAY_DINNER.getName());

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
                CreateGatheringRequest request =
                        new CreateGatheringRequest(GatheringFixture.FRIDAY_DINNER.getName());

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
    class ReissueJoinToken {

        @Test
        @DisplayName("모임을 만든 회원이면 새 참여 토큰과 함께 201을 반환한다")
        void shouldReissueJoinTokenWhenMemberIsHost() throws Exception {
            // given
            Member member = memberHelper.save(MemberFixture.KAKAO);
            String token = authHelper.bearerTokenFor(member);
            Gathering gathering =
                    gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());
            String oldJoinToken = gatheringHelper.issueJoinToken(gathering);

            // when & then
            mockMvc.perform(
                            post("/api/v1/gatherings/{gatheringId}/join-token", gathering.getId())
                                    .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.gatheringId").value(gathering.getId()))
                    .andExpect(jsonPath("$.data.joinToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.joinToken").value(not(oldJoinToken)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/gatherings/join/{joinToken}은")
    class GetJoinInfo {

        @Test
        @DisplayName("인증 없이도 모임 이름, 참여 가능 여부, 동의 안내 버전을 반환한다")
        void shouldReturnJoinInfoWithoutAuthentication() throws Exception {
            // given
            String joinToken = saveGatheringWithJoinToken();

            // when & then
            mockMvc.perform(get("/api/v1/gatherings/join/{joinToken}", joinToken))
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.data.name").value(GatheringFixture.FRIDAY_DINNER.getName()))
                    .andExpect(jsonPath("$.data.joinable").value(true))
                    .andExpect(
                            jsonPath("$.data.consentVersion")
                                    .value(GatheringPolicy.CONSENT_VERSION));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gatherings/join/{joinToken}은")
    class Join {

        @Test
        @DisplayName("인증 없이도 참여시키고 마스킹한 휴대폰과 개인 링크 토큰을 담아 201을 반환한다")
        void shouldJoinWithoutAuthentication() throws Exception {
            // given
            String joinToken = saveGatheringWithJoinToken();
            JoinGatheringRequest request =
                    joinRequest(
                            ParticipantFixture.KIM.getName(),
                            true,
                            GatheringPolicy.CONSENT_VERSION);

            // when & then
            mockMvc.perform(
                            post("/api/v1/gatherings/join/{joinToken}", joinToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andExpect(jsonPath("$.data.name").value(ParticipantFixture.KIM.getName()))
                    .andExpect(jsonPath("$.data.maskedPhoneNumber").value(MASKED_PHONE_NUMBER))
                    .andExpect(jsonPath("$.data.linkToken").isNotEmpty());
        }

        @Test
        @DisplayName("개인정보 수집 동의를 하지 않으면 400을 반환한다")
        void shouldReturnBadRequestWhenConsentNotAgreed() throws Exception {
            // given
            String joinToken = saveGatheringWithJoinToken();
            JoinGatheringRequest request =
                    joinRequest(
                            ParticipantFixture.KIM.getName(),
                            false,
                            GatheringPolicy.CONSENT_VERSION);

            // when & then
            mockMvc.perform(
                            post("/api/v1/gatherings/join/{joinToken}", joinToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("$.data.code")
                                    .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                    .andExpect(jsonPath("$.data.violations[0].field").value("consentAgreed"));
        }

        @Test
        @DisplayName("이름이 공백이면 400을 반환한다")
        void shouldReturnBadRequestWhenNameBlank() throws Exception {
            // given
            String joinToken = saveGatheringWithJoinToken();
            JoinGatheringRequest request = joinRequest(" ", true, GatheringPolicy.CONSENT_VERSION);

            // when & then
            mockMvc.perform(
                            post("/api/v1/gatherings/join/{joinToken}", joinToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.violations[0].field").value("name"));
        }

        @Test
        @DisplayName("동의 안내 버전이 공백이면 400을 반환한다")
        void shouldReturnBadRequestWhenConsentVersionBlank() throws Exception {
            // given
            String joinToken = saveGatheringWithJoinToken();
            JoinGatheringRequest request = joinRequest(ParticipantFixture.KIM.getName(), true, " ");

            // when & then
            mockMvc.perform(
                            post("/api/v1/gatherings/join/{joinToken}", joinToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.violations[0].field").value("consentVersion"));
        }

        private JoinGatheringRequest joinRequest(
                String name, boolean consentAgreed, String consentVersion) {
            return new JoinGatheringRequest(
                    name,
                    HYPHENATED_PHONE_NUMBER,
                    ParticipantFixture.KIM.isNonDrinker(),
                    consentAgreed,
                    consentVersion);
        }
    }

    private String saveGatheringWithJoinToken() {
        Member member = memberHelper.save(MemberFixture.KAKAO);
        Gathering gathering = gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

        return gatheringHelper.issueJoinToken(gathering);
    }
}
