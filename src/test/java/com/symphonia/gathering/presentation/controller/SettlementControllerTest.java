package com.symphonia.gathering.presentation.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.common.exception.error.CommonErrorCode;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.error.GatheringErrorCode;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.gathering.helper.SettlementHelper;
import com.symphonia.gathering.presentation.dto.request.FixedAmountRequest;
import com.symphonia.gathering.presentation.dto.request.RecordSettlementRequest;
import com.symphonia.gathering.presentation.dto.request.UpdateSettlementRequest;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class SettlementControllerTest extends IntegrationTest {

    private static final String MASKED_KIM_PHONE_NUMBER = "010-****-5678";

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
    @DisplayName("GET /api/v1/settlements/form은")
    class GetForm {

        @Nested
        @DisplayName("모임 참여자의 개인 링크 토큰인 경우")
        class WhenLinkTokenValid {

            @Test
            @DisplayName("인증 없이도 마스킹한 휴대폰을 담은 참여자, 인원 기본값, 결제자로 기록한 정산 건을 담아 200을 반환한다")
            void shouldReturnSettlementForm() throws Exception {
                // given
                settlementHelper.registerBankAccount(gathering, payer);
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);

                // when & then
                mockMvc.perform(
                                get("/api/v1/settlements/form")
                                        .header("X-Link-Token", payerLinkToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.recordable").value(true))
                        .andExpect(jsonPath("$.data.bankAccountRegistered").value(true))
                        .andExpect(
                                jsonPath("$.data.bankAccountConsentVersion")
                                        .value(SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION))
                        .andExpect(jsonPath("$.data.participants.length()").value(3))
                        .andExpect(
                                jsonPath("$.data.participants[0].name")
                                        .value(ParticipantFixture.KIM.getName()))
                        .andExpect(
                                jsonPath("$.data.participants[0].maskedPhoneNumber")
                                        .value(MASKED_KIM_PHONE_NUMBER))
                        .andExpect(jsonPath("$.data.participants[0].phoneNumber").doesNotExist())
                        .andExpect(
                                jsonPath("$.data.defaultParticipantIds")
                                        .value(
                                                contains(
                                                        payer.getId().intValue(),
                                                        lee.getId().intValue(),
                                                        park.getId().intValue())))
                        .andExpect(jsonPath("$.data.settlements[0].id").value(settlement.getId()))
                        .andExpect(jsonPath("$.data.settlements[0].payerAmount").value(3_334));
            }
        }

        @Nested
        @DisplayName("개인 링크 토큰에 해당하는 참여자가 없는 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("PARTICIPANT_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // when & then
                mockMvc.perform(
                                get("/api/v1/settlements/form")
                                        .header("X-Link-Token", "unknown-link-token"))
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.PARTICIPANT_NOT_FOUND.getCode()));
            }
        }

        @Nested
        @DisplayName("개인 링크 토큰 헤더가 없는 경우")
        class WhenLinkTokenHeaderMissing {

            @Test
            @DisplayName("MISSING_REQUEST_HEADER 코드와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // when & then
                mockMvc.perform(get("/api/v1/settlements/form"))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.MISSING_REQUEST_HEADER.getCode()));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/settlements는")
    class Record {

        @Nested
        @DisplayName("기록할 수 있는 요청인 경우")
        class WhenRecordable {

            @Test
            @DisplayName("인증 없이도 정산 건을 기록하고 분담을 담아 201을 반환한다")
            void shouldRecordSettlement() throws Exception {
                // given
                settlementHelper.registerBankAccount(gathering, payer);
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                10_000L,
                                participantIds(),
                                List.of(new FixedAmountRequest(lee.getId(), 1_050L)));

                // when & then
                mockMvc.perform(
                                post("/api/v1/settlements")
                                        .header("X-Link-Token", payerLinkToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.id").isNumber())
                        .andExpect(jsonPath("$.data.payerParticipantId").value(payer.getId()))
                        .andExpect(jsonPath("$.data.totalAmount").value(10_000))
                        .andExpect(jsonPath("$.data.payerAmount").value(4_475))
                        .andExpect(jsonPath("$.data.notified").value(false))
                        .andExpect(jsonPath("$.data.shares[0].participantId").value(lee.getId()))
                        .andExpect(jsonPath("$.data.shares[0].amount").value(1_050))
                        .andExpect(jsonPath("$.data.shares[0].fixed").value(true))
                        .andExpect(jsonPath("$.data.shares[0].status").value("UNPAID"))
                        .andExpect(jsonPath("$.data.shares[1].participantId").value(park.getId()))
                        .andExpect(jsonPath("$.data.shares[1].amount").value(4_475))
                        .andExpect(jsonPath("$.data.shares[1].fixed").value(false));
            }
        }

        @Nested
        @DisplayName("결제자가 계좌를 등록하지 않은 경우")
        class WhenBankAccountNotRegistered {

            @Test
            @DisplayName("BANK_ACCOUNT_REQUIRED 코드와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(10_000L, participantIds(), List.of());

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.BANK_ACCOUNT_REQUIRED.getCode()));
            }
        }

        @Nested
        @DisplayName("총액이 0인 경우")
        class WhenTotalAmountNotPositive {

            @Test
            @DisplayName("totalAmount 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(0L, participantIds(), List.of());

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("totalAmount"));
            }
        }

        @Nested
        @DisplayName("총액이 1억 원을 넘는 경우")
        class WhenTotalAmountExceedsLimit {

            @Test
            @DisplayName("totalAmount 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(100_000_001L, participantIds(), List.of());

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("totalAmount"));
            }
        }

        @Nested
        @DisplayName("지정 금액이 음수인 경우")
        class WhenFixedAmountNegative {

            @Test
            @DisplayName("지정 금액 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                10_000L,
                                participantIds(),
                                List.of(new FixedAmountRequest(lee.getId(), -1L)));

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(
                                jsonPath("$.data.violations[0].field")
                                        .value("fixedAmounts[0].amount"));
            }
        }

        @Nested
        @DisplayName("지정 금액이 1억 원을 넘는 경우")
        class WhenFixedAmountExceedsLimit {

            @Test
            @DisplayName("지정 금액 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                10_000L,
                                participantIds(),
                                List.of(new FixedAmountRequest(lee.getId(), 100_000_001L)));

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(
                                jsonPath("$.data.violations[0].field")
                                        .value("fixedAmounts[0].amount"));
            }
        }

        @Nested
        @DisplayName("같은 참여자의 금액을 두 번 지정한 경우")
        class WhenFixedAmountParticipantDuplicated {

            @Test
            @DisplayName("지정 대상 중복 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                10_000L,
                                participantIds(),
                                List.of(
                                        new FixedAmountRequest(lee.getId(), 1_000L),
                                        new FixedAmountRequest(lee.getId(), 2_000L)));

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(
                                jsonPath("$.data.violations[0].field")
                                        .value("fixedAmountParticipantsDistinct"));
            }
        }

        @Nested
        @DisplayName("인원이 비어 있는 경우")
        class WhenParticipantIdsEmpty {

            @Test
            @DisplayName("participantIds 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(10_000L, List.of(), List.of());

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("participantIds"));
            }
        }

        private ResultActions performRecord(RecordSettlementRequest request) throws Exception {
            return mockMvc.perform(
                    post("/api/v1/settlements")
                            .header("X-Link-Token", payerLinkToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/settlements/{settlementId}는")
    class Update {

        @Nested
        @DisplayName("결제자가 수정할 수 있는 정산 건인 경우")
        class WhenUpdatable {

            @Test
            @DisplayName("분담을 다시 계산한 정산 건을 담아 200을 반환한다")
            void shouldUpdateSettlement() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
                UpdateSettlementRequest request = updateRequest(20_000L);

                // when & then
                mockMvc.perform(
                                put("/api/v1/settlements/{settlementId}", settlement.getId())
                                        .header("X-Link-Token", payerLinkToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.id").value(settlement.getId()))
                        .andExpect(jsonPath("$.data.totalAmount").value(20_000))
                        .andExpect(jsonPath("$.data.payerAmount").value(10_000))
                        .andExpect(jsonPath("$.data.shares.length()").value(1))
                        .andExpect(jsonPath("$.data.shares[0].participantId").value(lee.getId()))
                        .andExpect(jsonPath("$.data.shares[0].amount").value(10_000));
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
                UpdateSettlementRequest request = updateRequest(20_000L);

                // when & then
                mockMvc.perform(
                                put("/api/v1/settlements/{settlementId}", settlement.getId())
                                        .header("X-Link-Token", leeLinkToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_SETTLEMENT_PAYER.getCode()));
            }
        }

        @Nested
        @DisplayName("총액이 0인 경우")
        class WhenTotalAmountNotPositive {

            @Test
            @DisplayName("totalAmount 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
                UpdateSettlementRequest request = updateRequest(0L);

                // when & then
                mockMvc.perform(
                                put("/api/v1/settlements/{settlementId}", settlement.getId())
                                        .header("X-Link-Token", payerLinkToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("totalAmount"));
            }
        }

        private UpdateSettlementRequest updateRequest(long totalAmount) {
            return new UpdateSettlementRequest(
                    totalAmount, List.of(payer.getId(), lee.getId()), List.of());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/settlements/{settlementId}/cancel은")
    class Cancel {

        @Nested
        @DisplayName("결제자가 취소할 수 있는 정산 건인 경우")
        class WhenCancellable {

            @Test
            @DisplayName("정산 건을 취소하고 200을 반환한다")
            void shouldCancelSettlement() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);

                // when & then
                mockMvc.perform(
                                post(
                                                "/api/v1/settlements/{settlementId}/cancel",
                                                settlement.getId())
                                        .header("X-Link-Token", payerLinkToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.ok").value(true));
            }
        }

        @Nested
        @DisplayName("이미 취소한 정산 건인 경우")
        class WhenSettlementCancelled {

            @Test
            @DisplayName("SETTLEMENT_ALREADY_CANCELLED 코드와 함께 409를 반환한다")
            void shouldReturnConflict() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.cancel(
                                settlementHelper.record(
                                        SettlementFixture.DINNER, gathering, payer));

                // when & then
                mockMvc.perform(
                                post(
                                                "/api/v1/settlements/{settlementId}/cancel",
                                                settlement.getId())
                                        .header("X-Link-Token", payerLinkToken))
                        .andExpect(status().isConflict())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.SETTLEMENT_ALREADY_CANCELLED
                                                        .getCode()));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/settlements/{settlementId}/shares/{participantId}/confirm은")
    class ConfirmShare {

        @Nested
        @DisplayName("결제자가 확인 완료로 바꿀 수 있는 분담인 경우")
        class WhenConfirmable {

            @Test
            @DisplayName("분담을 확인 완료로 바꾸고 확인한 금액과 보냈다는 금액을 담아 200을 반환한다")
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
        @DisplayName("이미 확인 완료인 분담인 경우")
        class WhenShareConfirmed {

            @Test
            @DisplayName("INVALID_SHARE_STATUS_TRANSITION 코드와 함께 409를 반환한다")
            void shouldReturnConflict() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.confirm(
                                settlementHelper.record(SettlementFixture.DINNER, gathering, payer),
                                lee);

                // when & then
                performConfirm(settlement, lee, payerLinkToken)
                        .andExpect(status().isConflict())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.INVALID_SHARE_STATUS_TRANSITION
                                                        .getCode()));
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
        @DisplayName("결제자가 미입금으로 되돌릴 수 있는 분담인 경우")
        class WhenRevertible {

            @Test
            @DisplayName("분담을 미입금으로 되돌리고 확인한 금액과 보냈다는 금액을 담아 200을 반환한다")
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
        @DisplayName("이미 미입금인 분담인 경우")
        class WhenShareUnpaid {

            @Test
            @DisplayName("INVALID_SHARE_STATUS_TRANSITION 코드와 함께 409를 반환한다")
            void shouldReturnConflict() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);

                // when & then
                performRevert(settlement, lee, payerLinkToken)
                        .andExpect(status().isConflict())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.INVALID_SHARE_STATUS_TRANSITION
                                                        .getCode()));
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

    private List<Long> participantIds() {
        return List.of(payer.getId(), lee.getId(), park.getId());
    }
}
