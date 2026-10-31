package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.result.PayerSharesResult;
import com.symphonia.gathering.application.dto.result.SettlementFormResult;
import com.symphonia.gathering.application.dto.result.SettlementParticipantResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.dto.result.ShareBreakdownResult;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.InvalidLinkTokenException;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.ShareStatus;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantBankAccountFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("SettlementQueryService 단위 테스트")
class SettlementQueryServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long SETTLEMENT_ID = 30L;
    private static final Long SECOND_SETTLEMENT_ID = 31L;
    private static final Long THIRD_SETTLEMENT_ID = 32L;
    private static final Long PAYER_ID = 20L;
    private static final Long LEE_ID = 21L;
    private static final Long PARK_ID = 22L;
    private static final List<Long> PARTICIPANT_IDS = List.of(PAYER_ID, LEE_ID, PARK_ID);
    private static final String LINK_TOKEN = "link-token";

    @InjectMocks private SettlementQueryService settlementQueryService;

    @Mock private ParticipantLinkTokenRepository participantLinkTokenRepository;
    @Mock private GatheringRepository gatheringRepository;
    @Mock private ParticipantBankAccountRepository participantBankAccountRepository;
    @Mock private SettlementRepository settlementRepository;

    @Nested
    @DisplayName("getForm 메서드는")
    class GetForm {

        @Nested
        @DisplayName("개인 링크 토큰에 해당하는 참여자가 없는 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던진다")
            void shouldThrowInvalidLinkTokenException() {
                // given
                given(participantLinkTokenRepository.findParticipantIdByValue(LINK_TOKEN))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> settlementQueryService.getForm(LINK_TOKEN))
                        .isInstanceOf(InvalidLinkTokenException.class);
            }
        }

        @Nested
        @DisplayName("모임의 첫 정산 건을 기록하는 경우")
        class WhenFirstSettlement {

            @Test
            @DisplayName("마스킹한 휴대폰을 담은 참여자 전원을 인원 기본값으로 하고 기록 가능, 계좌 미등록, 현재 계좌 동의 버전을 반환한다")
            void shouldReturnAllParticipantsAsDefault() {
                // given
                givenForm(LocalDateTime.now().minusHours(1), Optional.empty(), false, List.of());

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.recordable()).isTrue();
                assertThat(result.bankAccountRegistered()).isFalse();
                assertThat(result.bankAccountConsentVersion())
                        .isEqualTo(SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION);
                assertThat(result.participants())
                        .extracting(
                                SettlementParticipantResult::id,
                                SettlementParticipantResult::name,
                                SettlementParticipantResult::maskedPhoneNumber)
                        .containsExactly(
                                tuple(PAYER_ID, ParticipantFixture.KIM.getName(), "010-****-5678"),
                                tuple(LEE_ID, ParticipantFixture.LEE.getName(), "010-****-5432"),
                                tuple(PARK_ID, ParticipantFixture.PARK.getName(), "010-****-6666"));
                assertThat(result.defaultParticipantIds())
                        .containsExactly(PAYER_ID, LEE_ID, PARK_ID);
                assertThat(result.settlements()).isEmpty();
            }
        }

        @Nested
        @DisplayName("직전 정산 건이 있는 경우")
        class WhenLatestSettlementExists {

            @Test
            @DisplayName("직전 정산 건의 인원을 결제자부터 인원 기본값으로 반환한다")
            void shouldReturnLatestSettlementParticipantsAsDefault() {
                // given
                Settlement latest =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, LEE_ID, List.of(PAYER_ID, LEE_ID));
                givenForm(LocalDateTime.now().minusHours(1), Optional.of(latest), false, List.of());

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.defaultParticipantIds()).containsExactly(LEE_ID, PAYER_ID);
            }
        }

        @Nested
        @DisplayName("요청한 참여자가 결제자로 기록한 정산 건이 있는 경우")
        class WhenPayerSettlementsExist {

            @Test
            @DisplayName("계좌 등록 여부와 함께 그 정산 건을 분담과 함께 반환한다")
            void shouldReturnPayerSettlements() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID, PARK_ID));
                givenForm(
                        LocalDateTime.now().minusHours(1),
                        Optional.of(settlement),
                        true,
                        List.of(settlement));

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.bankAccountRegistered()).isTrue();
                assertThat(result.settlements()).hasSize(1);
                SettlementResult settlementResult = result.settlements().getFirst();
                assertThat(settlementResult.id()).isEqualTo(SETTLEMENT_ID);
                assertThat(settlementResult.payerParticipantId()).isEqualTo(PAYER_ID);
                assertThat(settlementResult.payerAmount()).isEqualTo(3_334L);
                assertThat(settlementResult.shares()).hasSize(2);
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 지난 경우")
        class WhenRecordingPeriodPassed {

            @Test
            @DisplayName("기록 불가로 표시한다")
            void shouldReturnNotRecordableForm() {
                // given
                givenForm(LocalDateTime.now().minusHours(49), Optional.empty(), true, List.of());

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.recordable()).isFalse();
            }
        }

        private void givenForm(
                LocalDateTime createdAt,
                Optional<Settlement> latestSettlement,
                boolean bankAccountRegistered,
                List<Settlement> payerSettlements) {
            given(participantLinkTokenRepository.findParticipantIdByValue(LINK_TOKEN))
                    .willReturn(Optional.of(PAYER_ID));
            given(gatheringRepository.findByParticipantId(PAYER_ID))
                    .willReturn(Optional.of(gathering(createdAt)));
            given(settlementRepository.findLatestByGatheringId(GATHERING_ID))
                    .willReturn(latestSettlement);
            given(participantBankAccountRepository.existsByParticipantId(PAYER_ID))
                    .willReturn(bankAccountRegistered);
            given(settlementRepository.findAllByPayerParticipantId(PAYER_ID))
                    .willReturn(payerSettlements);
        }
    }

    @Nested
    @DisplayName("getShares 메서드는")
    class GetShares {

        @Nested
        @DisplayName("개인 링크 토큰에 해당하는 참여자가 없는 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던진다")
            void shouldThrowInvalidLinkTokenException() {
                // given
                given(participantLinkTokenRepository.findParticipantIdByValue(LINK_TOKEN))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> settlementQueryService.getShares(LINK_TOKEN))
                        .isInstanceOf(InvalidLinkTokenException.class);
            }
        }

        @Nested
        @DisplayName("낼 분담이 없는 경우")
        class WhenSharesNotFound {

            @Test
            @DisplayName("결제자 계좌를 읽지 않고 빈 목록을 반환한다")
            void shouldReturnEmptyListWithoutReadingBankAccounts() {
                // given
                given(participantLinkTokenRepository.findParticipantIdByValue(LINK_TOKEN))
                        .willReturn(Optional.of(LEE_ID));
                given(gatheringRepository.findByParticipantId(LEE_ID))
                        .willReturn(Optional.of(gathering(LocalDateTime.now().minusHours(1))));
                given(settlementRepository.findAllByShareParticipantId(LEE_ID))
                        .willReturn(List.of());

                // when
                List<PayerSharesResult> result = settlementQueryService.getShares(LINK_TOKEN);

                // then
                assertThat(result).isEmpty();
                then(participantBankAccountRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("여러 결제자에게 낼 분담이 있는 경우")
        class WhenSharesOfPayersExist {

            @Test
            @DisplayName("결제자별로 묶어 결제자 이름과 계좌를 함께 반환한다")
            void shouldGroupSharesByPayer() {
                // given
                givenShares(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS),
                        SettlementFixture.SECOND_ROUND.createWithId(
                                SECOND_SETTLEMENT_ID, GATHERING_ID, PARK_ID, PARTICIPANT_IDS),
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                THIRD_SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID),
                                ShareStatus.SENT));

                // when
                List<PayerSharesResult> result = settlementQueryService.getShares(LINK_TOKEN);

                // then
                assertThat(result)
                        .extracting(
                                PayerSharesResult::payerParticipantId,
                                PayerSharesResult::payerName,
                                PayerSharesResult::bankName,
                                PayerSharesResult::accountNumber)
                        .containsExactly(
                                tuple(
                                        PAYER_ID,
                                        ParticipantFixture.KIM.getName(),
                                        ParticipantBankAccountFixture.KOOKMIN.getBankName(),
                                        ParticipantBankAccountFixture.KOOKMIN.getAccountNumber()),
                                tuple(
                                        PARK_ID,
                                        ParticipantFixture.PARK.getName(),
                                        ParticipantBankAccountFixture.KOOKMIN.getBankName(),
                                        ParticipantBankAccountFixture.KOOKMIN.getAccountNumber()));
                assertThat(result.getFirst().shares())
                        .extracting(ShareBreakdownResult::settlementId)
                        .containsExactly(SETTLEMENT_ID, THIRD_SETTLEMENT_ID);
                assertThat(result.getLast().shares())
                        .extracting(ShareBreakdownResult::settlementId)
                        .containsExactly(SECOND_SETTLEMENT_ID);
            }

            @Test
            @DisplayName("미입금 분담이 남은 결제자를 먼저 기록한 결제자보다 앞에 둔다")
            void shouldPutPayersWithUnpaidSharesFirst() {
                // given
                givenShares(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT),
                        SettlementFixture.SECOND_ROUND.createWithId(
                                SECOND_SETTLEMENT_ID, GATHERING_ID, PARK_ID, PARTICIPANT_IDS));

                // when
                List<PayerSharesResult> result = settlementQueryService.getShares(LINK_TOKEN);

                // then
                assertThat(result)
                        .extracting(
                                PayerSharesResult::payerParticipantId,
                                PayerSharesResult::hasUnpaidShares)
                        .containsExactly(tuple(PARK_ID, true), tuple(PAYER_ID, false));
            }

            @Test
            @DisplayName("0원으로 지정해 확인 완료로 시작한 분담만 있는 결제자는 낼 분담이 없어 뒤에 둔다")
            void shouldPutPayerWithOnlyZeroShareLast() {
                // given
                givenShares(
                        SettlementFixture.DINNER.createWithFixedAmounts(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                Map.of(LEE_ID, 0L)),
                        SettlementFixture.SECOND_ROUND.createWithId(
                                SECOND_SETTLEMENT_ID, GATHERING_ID, PARK_ID, PARTICIPANT_IDS));

                // when
                List<PayerSharesResult> result = settlementQueryService.getShares(LINK_TOKEN);

                // then
                assertThat(result)
                        .extracting(
                                PayerSharesResult::payerParticipantId,
                                PayerSharesResult::unpaidAmount,
                                PayerSharesResult::hasUnpaidShares)
                        .containsExactly(tuple(PARK_ID, 6_666L, true), tuple(PAYER_ID, 0L, false));
                assertThat(result.getLast().shares())
                        .extracting(ShareBreakdownResult::amount, ShareBreakdownResult::status)
                        .containsExactly(tuple(0L, ShareStatus.CONFIRMED));
            }

            @Test
            @DisplayName("결제자 계좌를 결제자마다 따로 읽지 않고 한 번에 읽는다")
            void shouldReadPayerBankAccountsAtOnce() {
                // given
                givenShares(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS),
                        SettlementFixture.SECOND_ROUND.createWithId(
                                SECOND_SETTLEMENT_ID, GATHERING_ID, PARK_ID, PARTICIPANT_IDS));

                // when
                settlementQueryService.getShares(LINK_TOKEN);

                // then
                ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.captor();
                then(participantBankAccountRepository)
                        .should()
                        .findAllByParticipantIds(captor.capture(), eq(GATHERING_ID));
                assertThat(captor.getValue()).containsExactlyInAnyOrder(PAYER_ID, PARK_ID);
            }
        }

        @Nested
        @DisplayName("입금 상태가 섞인 분담이 있는 경우")
        class WhenSharesHaveMixedStatuses {

            @Test
            @DisplayName("미입금 분담의 금액만 더해 미입금 금액으로 반환한다")
            void shouldSumOnlyUnpaidShares() {
                // given
                givenShares(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS),
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID),
                                ShareStatus.SENT),
                        SettlementFixture.SECOND_ROUND.createWithFirstShareStatus(
                                THIRD_SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.CONFIRMED));

                // when
                List<PayerSharesResult> result = settlementQueryService.getShares(LINK_TOKEN);

                // then
                PayerSharesResult payerShares = result.getFirst();
                assertThat(payerShares.unpaidAmount()).isEqualTo(3_333L);
                assertThat(payerShares.hasUnpaidShares()).isTrue();
                assertThat(payerShares.shares())
                        .extracting(ShareBreakdownResult::amount, ShareBreakdownResult::status)
                        .containsExactly(
                                tuple(3_333L, ShareStatus.UNPAID),
                                tuple(5_000L, ShareStatus.SENT),
                                tuple(6_666L, ShareStatus.CONFIRMED));
            }
        }

        @Nested
        @DisplayName("결제자가 금액을 지정한 정산 건인 경우")
        class WhenAmountFixed {

            @Test
            @DisplayName("다른 참여자의 금액 없이 본인 분담과 총액, 인원, 지정 여부, 입금 상태를 계산 근거로 반환한다")
            void shouldReturnOwnShareBreakdown() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.createWithFixedAmounts(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                Map.of(LEE_ID, 1_050L));
                givenShares(settlement);

                // when
                List<PayerSharesResult> result = settlementQueryService.getShares(LINK_TOKEN);

                // then
                assertThat(result.getFirst().shares())
                        .singleElement()
                        .isEqualTo(
                                new ShareBreakdownResult(
                                        SETTLEMENT_ID,
                                        SettlementFixture.DINNER.getTotalAmount(),
                                        3,
                                        true,
                                        1_050L,
                                        ShareStatus.UNPAID,
                                        SettlementFixture.DINNER.getRecordedAt()));
            }
        }

        private void givenShares(Settlement... settlements) {
            given(participantLinkTokenRepository.findParticipantIdByValue(LINK_TOKEN))
                    .willReturn(Optional.of(LEE_ID));
            given(gatheringRepository.findByParticipantId(LEE_ID))
                    .willReturn(Optional.of(gathering(LocalDateTime.now().minusHours(1))));
            given(settlementRepository.findAllByShareParticipantId(LEE_ID))
                    .willReturn(List.of(settlements));
            given(participantBankAccountRepository.findAllByParticipantIds(any(), eq(GATHERING_ID)))
                    .willReturn(
                            List.of(
                                    ParticipantBankAccountFixture.KOOKMIN.create(PAYER_ID),
                                    ParticipantBankAccountFixture.KOOKMIN.create(PARK_ID)));
        }
    }

    private Gathering gathering(LocalDateTime createdAt) {
        return GatheringFixture.FRIDAY_DINNER.createWithIdAndCreatedAt(
                GATHERING_ID,
                createdAt,
                List.of(
                        ParticipantFixture.KIM.createWithId(PAYER_ID),
                        ParticipantFixture.LEE.createWithId(LEE_ID),
                        ParticipantFixture.PARK.createWithId(PARK_ID)));
    }
}
