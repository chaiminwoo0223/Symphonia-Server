package com.symphonia.gathering.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.error.GatheringErrorCode;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.gathering.helper.SettlementHelper;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

class SettlementControllerTest extends IntegrationTest {

    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private SettlementHelper settlementHelper;
    @Autowired private MemberHelper memberHelper;

    private Gathering gathering;
    private Participant payer;
    private Participant lee;
    private Participant park;
    private String payerLinkToken;

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
        payerLinkToken = gatheringHelper.issueLinkToken(payer);
    }

    @Nested
    @DisplayName("POST /api/v1/settlements/{settlementId}/shares/{participantId}/confirm은")
    class ConfirmShare {

        @Nested
        @DisplayName("결제자가 확인 완료로 바꿀 수 있는 분담금인 경우")
        class WhenConfirmable {

            @Test
            @DisplayName("분담금을 확인 완료로 바꾸고 확인한 금액과 보냈다는 금액을 담아 200을 반환한다")
            void shouldConfirmShare() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.markSent(
                                settlementHelper.record(SettlementFixture.DINNER, gathering, payer),
                                lee);

                // when & then
                performConfirm(settlement, park, payerLinkToken)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.id").value(settlement.getId()))
                        .andExpect(jsonPath("$.data.roundId").value(roundId()))
                        .andExpect(jsonPath("$.data.roundNumber").value(1))
                        .andExpect(jsonPath("$.data.shares[0].participantId").value(lee.getId()))
                        .andExpect(jsonPath("$.data.shares[0].status").value("SENT"))
                        .andExpect(jsonPath("$.data.shares[1].participantId").value(park.getId()))
                        .andExpect(jsonPath("$.data.shares[1].status").value("CONFIRMED"))
                        .andExpect(jsonPath("$.data.confirmedAmount").value(3_333))
                        .andExpect(jsonPath("$.data.sentAmount").value(3_333));
            }
        }

        @Nested
        @DisplayName("결제자가 아닌 참여자의 요청인 경우")
        class WhenParticipantIsNotPayer {

            @Test
            @DisplayName("NOT_SETTLEMENT_PAYER 코드와 함께 403을 반환한다")
            void shouldReturnForbidden() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
                String leeLinkToken = gatheringHelper.issueLinkToken(lee);

                // when & then
                performConfirm(settlement, lee, leeLinkToken)
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_SETTLEMENT_PAYER.getCode()));
            }
        }

        @Nested
        @DisplayName("이미 확인 완료인 분담금인 경우")
        class WhenShareConfirmed {

            @Test
            @DisplayName("같은 요청을 다시 보낸 것으로 보고 확인 완료인 현재 상태를 담아 200을 반환한다")
            void shouldReturnOk() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.confirm(
                                settlementHelper.record(SettlementFixture.DINNER, gathering, payer),
                                lee);

                // when & then
                performConfirm(settlement, lee, payerLinkToken)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.shares[0].participantId").value(lee.getId()))
                        .andExpect(jsonPath("$.data.shares[0].status").value("CONFIRMED"));
            }
        }

        private ResultActions performConfirm(
                Settlement settlement, Participant participant, String linkToken) throws Exception {
            return mockMvc.perform(
                    post(
                                    "/api/v1/settlements/{settlementId}/shares/{participantId}/confirm",
                                    settlement.getId(),
                                    participant.getId())
                            .header("X-Link-Token", linkToken));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/settlements/{settlementId}/shares/{participantId}/unpaid는")
    class RevertShare {

        @Nested
        @DisplayName("결제자가 미입금으로 되돌릴 수 있는 분담금인 경우")
        class WhenRevertible {

            @Test
            @DisplayName("분담금을 미입금으로 되돌리고 확인한 금액과 보냈다는 금액을 담아 200을 반환한다")
            void shouldRevertShare() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.confirm(
                                settlementHelper.markSent(
                                        settlementHelper.record(
                                                SettlementFixture.DINNER, gathering, payer),
                                        lee),
                                park);

                // when & then
                performRevert(settlement, lee, payerLinkToken)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.id").value(settlement.getId()))
                        .andExpect(jsonPath("$.data.roundId").value(roundId()))
                        .andExpect(jsonPath("$.data.roundNumber").value(1))
                        .andExpect(jsonPath("$.data.shares[0].participantId").value(lee.getId()))
                        .andExpect(jsonPath("$.data.shares[0].status").value("UNPAID"))
                        .andExpect(jsonPath("$.data.shares[1].status").value("CONFIRMED"))
                        .andExpect(jsonPath("$.data.confirmedAmount").value(3_333))
                        .andExpect(jsonPath("$.data.sentAmount").value(0));
            }
        }

        @Nested
        @DisplayName("결제자가 아닌 참여자의 요청인 경우")
        class WhenParticipantIsNotPayer {

            @Test
            @DisplayName("NOT_SETTLEMENT_PAYER 코드와 함께 403을 반환한다")
            void shouldReturnForbidden() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.markSent(
                                settlementHelper.record(SettlementFixture.DINNER, gathering, payer),
                                lee);
                String leeLinkToken = gatheringHelper.issueLinkToken(lee);

                // when & then
                performRevert(settlement, lee, leeLinkToken)
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_SETTLEMENT_PAYER.getCode()));
            }
        }

        @Nested
        @DisplayName("이미 미입금인 분담금인 경우")
        class WhenShareUnpaid {

            @Test
            @DisplayName("같은 요청을 다시 보낸 것으로 보고 미입금인 현재 상태를 담아 200을 반환한다")
            void shouldReturnOk() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);

                // when & then
                performRevert(settlement, lee, payerLinkToken)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.shares[0].participantId").value(lee.getId()))
                        .andExpect(jsonPath("$.data.shares[0].status").value("UNPAID"));
            }
        }

        private ResultActions performRevert(
                Settlement settlement, Participant participant, String linkToken) throws Exception {
            return mockMvc.perform(
                    post(
                                    "/api/v1/settlements/{settlementId}/shares/{participantId}/unpaid",
                                    settlement.getId(),
                                    participant.getId())
                            .header("X-Link-Token", linkToken));
        }
    }

    private Long roundId() {
        return gathering.getRounds().getFirst().getId();
    }
}
