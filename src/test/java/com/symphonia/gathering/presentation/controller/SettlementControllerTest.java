package com.symphonia.gathering.presentation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.common.exception.error.CommonErrorCode;
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
import java.time.LocalDateTime;
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
    @DisplayName("GET /api/v1/settlements는")
    class GetCollection {

        @Nested
        @DisplayName("결제자의 개인 링크 토큰인 경우")
        class WhenPayerLinkToken {

            @Test
            @DisplayName("인증 없이도 참여자별 이름과 전체 휴대폰, 차수별 금액과 상태, 합계를 담아 200을 반환한다")
            void shouldReturnCollectionGroupedByParticipant() throws Exception {
                // given
                Gathering withSecondRound = gatheringHelper.addRoundsUpTo(gathering, 2);
                Settlement dinner =
                        settlementHelper.record(SettlementFixture.DINNER, withSecondRound, payer);
                Settlement secondRound =
                        settlementHelper.markSent(
                                settlementHelper.record(
                                        SettlementFixture.SECOND_ROUND,
                                        withSecondRound,
                                        withSecondRound.getRounds().get(1),
                                        payer),
                                lee);

                // when & then
                performGetCollection(payerLinkToken)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.deletesAt").isNotEmpty())
                        .andExpect(jsonPath("$.data.totalAmount").value(19_998))
                        .andExpect(jsonPath("$.data.participants.length()").value(2))
                        .andExpect(
                                jsonPath("$.data.participants[0].participantId").value(lee.getId()))
                        .andExpect(
                                jsonPath("$.data.participants[0].name")
                                        .value(ParticipantFixture.LEE.getName()))
                        .andExpect(
                                jsonPath("$.data.participants[0].phoneNumber")
                                        .value(ParticipantFixture.LEE.getPhoneNumber()))
                        .andExpect(jsonPath("$.data.participants[0].totalAmount").value(9_999))
                        .andExpect(jsonPath("$.data.participants[0].shares.length()").value(2))
                        .andExpect(
                                jsonPath("$.data.participants[0].shares[0].settlementId")
                                        .value(dinner.getId()))
                        .andExpect(
                                jsonPath("$.data.participants[0].shares[0].roundId")
                                        .value(withSecondRound.getRounds().get(0).getId()))
                        .andExpect(
                                jsonPath("$.data.participants[0].shares[0].roundNumber").value(1))
                        .andExpect(jsonPath("$.data.participants[0].shares[0].amount").value(3_333))
                        .andExpect(
                                jsonPath("$.data.participants[0].shares[0].status").value("UNPAID"))
                        .andExpect(
                                jsonPath("$.data.participants[0].shares[1].settlementId")
                                        .value(secondRound.getId()))
                        .andExpect(
                                jsonPath("$.data.participants[0].shares[1].roundId")
                                        .value(withSecondRound.getRounds().get(1).getId()))
                        .andExpect(
                                jsonPath("$.data.participants[0].shares[1].roundNumber").value(2))
                        .andExpect(jsonPath("$.data.participants[0].shares[1].amount").value(6_666))
                        .andExpect(
                                jsonPath("$.data.participants[0].shares[1].status").value("SENT"))
                        .andExpect(
                                jsonPath("$.data.participants[1].participantId")
                                        .value(park.getId()))
                        .andExpect(
                                jsonPath("$.data.participants[1].phoneNumber")
                                        .value(ParticipantFixture.PARK.getPhoneNumber()))
                        .andExpect(jsonPath("$.data.participants[1].totalAmount").value(9_999))
                        .andExpect(
                                jsonPath("$.data.participants[1].shares[1].status")
                                        .value("UNPAID"));
            }

            @Test
            @DisplayName("취소한 정산 건은 담지 않는다")
            void shouldExcludeCancelledSettlement() throws Exception {
                // given
                settlementHelper.cancel(
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer));
                Settlement secondRound =
                        settlementHelper.record(SettlementFixture.SECOND_ROUND, gathering, payer);

                // when & then
                performGetCollection(payerLinkToken)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalAmount").value(13_332))
                        .andExpect(jsonPath("$.data.participants.length()").value(2))
                        .andExpect(jsonPath("$.data.participants[0].shares.length()").value(1))
                        .andExpect(
                                jsonPath("$.data.participants[0].shares[0].settlementId")
                                        .value(secondRound.getId()))
                        .andExpect(jsonPath("$.data.participants[0].totalAmount").value(6_666));
            }

            @Test
            @DisplayName("다른 참여자가 결제자인 정산 건은 섞지 않고 결제자로 지정된 정산 건만 담는다")
            void shouldExcludeSettlementsOfOtherPayers() throws Exception {
                // given
                Settlement dinner =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
                settlementHelper.record(SettlementFixture.SECOND_ROUND, gathering, park);

                // when & then
                performGetCollection(payerLinkToken)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalAmount").value(6_666))
                        .andExpect(jsonPath("$.data.participants.length()").value(2))
                        .andExpect(
                                jsonPath("$.data.participants[0].participantId").value(lee.getId()))
                        .andExpect(jsonPath("$.data.participants[0].shares.length()").value(1))
                        .andExpect(
                                jsonPath("$.data.participants[0].shares[0].settlementId")
                                        .value(dinner.getId()))
                        .andExpect(
                                jsonPath("$.data.participants[1].participantId")
                                        .value(park.getId()))
                        .andExpect(jsonPath("$.data.participants[1].shares.length()").value(1))
                        .andExpect(
                                jsonPath("$.data.participants[1].shares[0].settlementId")
                                        .value(dinner.getId()));
            }
        }

        @Nested
        @DisplayName("결제자로 지정된 정산 건이 없는 참여자의 개인 링크 토큰인 경우")
        class WhenParticipantIsNotPayer {

            @Test
            @DisplayName("빈 목록과 0원 합계를 담아 200을 반환한다")
            void shouldReturnEmptyParticipants() throws Exception {
                // given
                settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
                String leeLinkToken = gatheringHelper.issueLinkToken(lee);

                // when & then
                performGetCollection(leeLinkToken)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.deletesAt").isNotEmpty())
                        .andExpect(jsonPath("$.data.totalAmount").value(0))
                        .andExpect(jsonPath("$.data.participants").isEmpty());
            }
        }

        @Nested
        @DisplayName("파기 시각이 지난 모임의 링크 토큰인 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName(
                    "결제자로 지정된 정산 건이 있어도 참여자의 전체 휴대폰을 담지 않고 LINK_TOKEN_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFoundWithoutPhoneNumbers() throws Exception {
                // given
                Gathering expiredGathering =
                        gatheringHelper.join(
                                gatheringHelper.save(
                                        GatheringFixture.FRIDAY_DINNER,
                                        gathering.getHostMemberId(),
                                        LocalDateTime.now().minusDays(30)),
                                ParticipantFixture.KIM,
                                ParticipantFixture.LEE,
                                ParticipantFixture.PARK);
                Participant expiredPayer = expiredGathering.getParticipants().get(0);
                settlementHelper.record(SettlementFixture.DINNER, expiredGathering, expiredPayer);
                String expiredPayerLinkToken = gatheringHelper.issueLinkToken(expiredPayer);

                // when & then
                performGetCollection(expiredPayerLinkToken)
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.LINK_TOKEN_NOT_FOUND.getCode()))
                        .andExpect(
                                content()
                                        .string(
                                                not(
                                                        containsString(
                                                                ParticipantFixture.LEE
                                                                        .getPhoneNumber()))))
                        .andExpect(
                                content()
                                        .string(
                                                not(
                                                        containsString(
                                                                ParticipantFixture.PARK
                                                                        .getPhoneNumber()))));
            }
        }

        @Nested
        @DisplayName("개인 링크 토큰으로 참여자를 찾지 못한 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("LINK_TOKEN_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // when & then
                performGetCollection("unknown-link-token")
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
                // when & then
                mockMvc.perform(get("/api/v1/settlements"))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.MISSING_REQUEST_HEADER.getCode()));
            }
        }

        private ResultActions performGetCollection(String linkToken) throws Exception {
            return mockMvc.perform(get("/api/v1/settlements").header("X-Link-Token", linkToken));
        }
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
