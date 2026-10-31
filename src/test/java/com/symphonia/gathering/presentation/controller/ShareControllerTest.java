package com.symphonia.gathering.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.common.exception.error.CommonErrorCode;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.error.GatheringErrorCode;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantBankAccountFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.gathering.helper.SettlementHelper;
import com.symphonia.gathering.presentation.dto.request.MarkSharesSentRequest;
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

class ShareControllerTest extends IntegrationTest {

    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private SettlementHelper settlementHelper;
    @Autowired private MemberHelper memberHelper;

    private Gathering gathering;
    private Participant payer;
    private Participant lee;
    private Participant park;
    private String leeLinkToken;

    @BeforeEach
    void setUp() {
        Member member = memberHelper.save(MemberFixture.KAKAO);
        gathering =
                gatheringHelper.join(
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId()),
                        ParticipantFixture.KIM,
                        ParticipantFixture.LEE,
                        ParticipantFixture.PARK);
        payer = gathering.getParticipants().get(0);
        lee = gathering.getParticipants().get(1);
        park = gathering.getParticipants().get(2);
        leeLinkToken = gatheringHelper.issueLinkToken(lee);
        settlementHelper.registerBankAccount(gathering, payer);
        settlementHelper.registerBankAccount(gathering, park);
    }

    @Nested
    @DisplayName("GET /api/v1/shares는")
    class GetShares {

        @Nested
        @DisplayName("모임 참여자의 개인 링크 토큰인 경우")
        class WhenLinkTokenValid {

            @Test
            @DisplayName("인증 없이도 결제자별로 묶은 분담금을 결제자 계좌, 계산 근거와 함께 담아 200을 반환한다")
            void shouldReturnSharesGroupedByPayer() throws Exception {
                // given
                Settlement dinner =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
                Settlement secondRound =
                        settlementHelper.record(SettlementFixture.SECOND_ROUND, gathering, park);

                // when & then
                mockMvc.perform(get("/api/v1/shares").header("X-Link-Token", leeLinkToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.length()").value(2))
                        .andExpect(jsonPath("$.data[0].payerParticipantId").value(payer.getId()))
                        .andExpect(
                                jsonPath("$.data[0].payerName")
                                        .value(ParticipantFixture.KIM.getName()))
                        .andExpect(
                                jsonPath("$.data[0].bankName")
                                        .value(ParticipantBankAccountFixture.KOOKMIN.getBankName()))
                        .andExpect(
                                jsonPath("$.data[0].accountNumber")
                                        .value(
                                                ParticipantBankAccountFixture.KOOKMIN
                                                        .getAccountNumber()))
                        .andExpect(jsonPath("$.data[0].unpaidAmount").value(3_333))
                        .andExpect(jsonPath("$.data[0].hasUnpaidShares").value(true))
                        .andExpect(jsonPath("$.data[0].shares.length()").value(1))
                        .andExpect(
                                jsonPath("$.data[0].shares[0].settlementId").value(dinner.getId()))
                        .andExpect(jsonPath("$.data[0].shares[0].totalAmount").value(10_000))
                        .andExpect(jsonPath("$.data[0].shares[0].headcount").value(3))
                        .andExpect(jsonPath("$.data[0].shares[0].fixed").value(false))
                        .andExpect(jsonPath("$.data[0].shares[0].amount").value(3_333))
                        .andExpect(jsonPath("$.data[0].shares[0].status").value("UNPAID"))
                        .andExpect(jsonPath("$.data[0].shares[0].recordedAt").isNotEmpty())
                        .andExpect(jsonPath("$.data[1].payerParticipantId").value(park.getId()))
                        .andExpect(jsonPath("$.data[1].unpaidAmount").value(6_666))
                        .andExpect(
                                jsonPath("$.data[1].shares[0].settlementId")
                                        .value(secondRound.getId()));
            }
        }

        @Nested
        @DisplayName("개인 링크 토큰 헤더가 없는 경우")
        class WhenLinkTokenHeaderMissing {

            @Test
            @DisplayName("MISSING_REQUEST_HEADER 코드와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // when & then
                mockMvc.perform(get("/api/v1/shares"))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.MISSING_REQUEST_HEADER.getCode()));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/shares/sent는")
    class MarkSent {

        @Nested
        @DisplayName("그 결제자에게 낼 미입금 분담금이 있는 경우")
        class WhenUnpaidSharesExist {

            @Test
            @DisplayName("분담금을 보냄으로 바꾸고 그 결제자의 묶음을 담아 200을 반환한다")
            void shouldMarkSharesSent() throws Exception {
                // given
                Settlement dinner =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);

                // when & then
                performMarkSent(new MarkSharesSentRequest(payer.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.payerParticipantId").value(payer.getId()))
                        .andExpect(
                                jsonPath("$.data.accountNumber")
                                        .value(
                                                ParticipantBankAccountFixture.KOOKMIN
                                                        .getAccountNumber()))
                        .andExpect(jsonPath("$.data.unpaidAmount").value(0))
                        .andExpect(jsonPath("$.data.hasUnpaidShares").value(false))
                        .andExpect(jsonPath("$.data.shares[0].settlementId").value(dinner.getId()))
                        .andExpect(jsonPath("$.data.shares[0].status").value("SENT"));
            }
        }

        @Nested
        @DisplayName("결제자 id가 없는 경우")
        class WhenPayerParticipantIdMissing {

            @Test
            @DisplayName("payerParticipantId 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // when & then
                performMarkSent(new MarkSharesSentRequest(null))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(
                                jsonPath("$.data.violations[0].field").value("payerParticipantId"));
            }
        }

        @Nested
        @DisplayName("그 결제자에게 낼 분담금이 없는 경우")
        class WhenSharesNotFound {

            @Test
            @DisplayName("SETTLEMENT_SHARE_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // given
                settlementHelper.record(SettlementFixture.DINNER, gathering, payer);

                // when & then
                performMarkSent(new MarkSharesSentRequest(park.getId()))
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.SETTLEMENT_SHARE_NOT_FOUND
                                                        .getCode()));
            }
        }

        private ResultActions performMarkSent(MarkSharesSentRequest request) throws Exception {
            return mockMvc.perform(
                    post("/api/v1/shares/sent")
                            .header("X-Link-Token", leeLinkToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
        }
    }
}
