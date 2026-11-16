package com.symphonia.gathering.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.common.exception.error.CommonErrorCode;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.error.GatheringErrorCode;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.gathering.presentation.dto.request.AddRoundRequest;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class RoundControllerTest extends IntegrationTest {

    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private MemberHelper memberHelper;

    private Gathering gathering;
    private String linkToken;

    @BeforeEach
    void setUp() {
        Member member = memberHelper.save(MemberFixture.KAKAO);
        gathering =
                gatheringHelper.join(
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId()),
                        ParticipantFixture.KIM);
        linkToken = gatheringHelper.issueLinkToken(gathering.getParticipants().getFirst());
    }

    @Nested
    @DisplayName("POST /api/v1/rounds는")
    class Add {

        @Nested
        @DisplayName("마지막 차수의 다음 번호를 요청한 경우")
        class WhenNextRoundNumber {

            @Test
            @DisplayName("인증 없이도 차수를 추가하고 그 차수의 id와 번호를 담아 201을 반환한다")
            void shouldAddRoundWithoutAuthentication() throws Exception {
                // given
                AddRoundRequest request = new AddRoundRequest(2);

                // when & then
                performAdd(request, linkToken)
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.id").isNumber())
                        .andExpect(jsonPath("$.data.number").value(2));
            }
        }

        @Nested
        @DisplayName("이미 있는 번호를 요청한 경우")
        class WhenRoundAlreadyExists {

            @Test
            @DisplayName("새로 만들지 않고 그 차수의 id와 번호를 담아 201을 반환한다")
            void shouldReturnExistingRound() throws Exception {
                // given
                AddRoundRequest request = new AddRoundRequest(1);

                // when & then
                performAdd(request, linkToken)
                        .andExpect(status().isCreated())
                        .andExpect(
                                jsonPath("$.data.id")
                                        .value(gathering.getRounds().getFirst().getId()))
                        .andExpect(jsonPath("$.data.number").value(1));
            }
        }

        @Nested
        @DisplayName("차수 번호가 0인 경우")
        class WhenRoundNumberNotPositive {

            @Test
            @DisplayName("roundNumber 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                AddRoundRequest request = new AddRoundRequest(0);

                // when & then
                performAdd(request, linkToken)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("roundNumber"));
            }
        }

        @Nested
        @DisplayName("마지막 차수의 다음 번호보다 큰 번호를 요청한 경우")
        class WhenRoundNumberSkipsRound {

            @Test
            @DisplayName("INVALID_GATHERING_ROUND_NUMBER 코드와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                AddRoundRequest request = new AddRoundRequest(3);

                // when & then
                performAdd(request, linkToken)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.INVALID_GATHERING_ROUND_NUMBER
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("차수가 이미 상한까지 있는 경우")
        class WhenRoundCountAtLimit {

            @Test
            @DisplayName("GATHERING_ROUND_COUNT_EXCEEDS_LIMIT 코드와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                gatheringHelper.addRoundsUpTo(gathering, GatheringPolicy.MAX_ROUND_COUNT);
                AddRoundRequest request = new AddRoundRequest(GatheringPolicy.MAX_ROUND_COUNT + 1);

                // when & then
                performAdd(request, linkToken)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode
                                                        .GATHERING_ROUND_COUNT_EXCEEDS_LIMIT
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("개인 링크 토큰에 해당하는 참여자가 없는 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("LINK_TOKEN_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // given
                AddRoundRequest request = new AddRoundRequest(2);

                // when & then
                performAdd(request, "unknown-link-token")
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.LINK_TOKEN_NOT_FOUND.getCode()));
            }
        }

        @Nested
        @DisplayName("개인 링크 토큰 헤더가 없는 경우")
        class WhenLinkTokenHeaderMissing {

            @Test
            @DisplayName("MISSING_REQUEST_HEADER 코드와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                AddRoundRequest request = new AddRoundRequest(2);

                // when & then
                mockMvc.perform(
                                post("/api/v1/rounds")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.MISSING_REQUEST_HEADER.getCode()));
            }
        }

        private ResultActions performAdd(AddRoundRequest request, String linkToken)
                throws Exception {
            return mockMvc.perform(
                    post("/api/v1/rounds")
                            .header("X-Link-Token", linkToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
        }
    }
}
