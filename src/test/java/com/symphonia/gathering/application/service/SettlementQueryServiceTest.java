package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.result.GatheringRoundResult;
import com.symphonia.gathering.application.dto.result.LinkedParticipantResult;
import com.symphonia.gathering.application.dto.result.PayerSharesResult;
import com.symphonia.gathering.application.dto.result.SettlementFormResult;
import com.symphonia.gathering.application.dto.result.SettlementParticipantResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.dto.result.ShareBreakdownResult;
import com.symphonia.gathering.application.dto.result.SharesResult;
import com.symphonia.gathering.application.usecase.GetLinkedParticipantUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.LinkTokenNotFoundException;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.ShareStatus;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringRoundFixture;
import com.symphonia.gathering.fixture.ParticipantBankAccountFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.global.config.TimeConfig;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@DisplayName("SettlementQueryService 단위 테스트")
class SettlementQueryServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long ROUND_ID = GatheringRoundFixture.FIRST.getId();
    private static final Long SETTLEMENT_ID = 30L;
    private static final Long SECOND_SETTLEMENT_ID = 31L;
    private static final Long THIRD_SETTLEMENT_ID = 32L;
    private static final Long PAYER_ID = 20L;
    private static final Long LEE_ID = 21L;
    private static final Long PARK_ID = 22L;
    // 모임을 읽은 뒤에 참여해서 읽은 모임의 참여자에 없는 참여자다.
    private static final Long NEWCOMER_ID = 23L;
    private static final List<Long> PARTICIPANT_IDS = List.of(PAYER_ID, LEE_ID, PARK_ID);
    private static final String LINK_TOKEN = "link-token";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 18, 0);
    private static final LocalDateTime DELETES_AT = NOW.plusDays(7);

    // 기록 기간은 모임을 만든 지 48시간이 되는 순간 닫힌다.
    // 닫히기 1초 전과 닫히는 순간을 경계로 쓴다.
    private static final LocalDateTime OPEN_PERIOD_OPENED_AT = NOW.minusHours(48).plusSeconds(1);
    private static final LocalDateTime CLOSED_PERIOD_OPENED_AT = NOW.minusHours(48);

    private SettlementQueryService settlementQueryService;

    @Mock private GetLinkedParticipantUseCase getLinkedParticipantUseCase;
    @Mock private GatheringRepository gatheringRepository;
    @Mock private ParticipantBankAccountRepository participantBankAccountRepository;
    @Mock private SettlementRepository settlementRepository;

    @BeforeEach
    void setUp() {
        Clock clock =
                Clock.fixed(
                        NOW.atZone(TimeConfig.SERVICE_ZONE).toInstant(), TimeConfig.SERVICE_ZONE);
        settlementQueryService =
                new SettlementQueryService(
                        getLinkedParticipantUseCase,
                        gatheringRepository,
                        participantBankAccountRepository,
                        settlementRepository,
                        clock);
    }

    @Nested
    @DisplayName("getForm 메서드는")
    class GetForm {

        // 토큰이 없거나 모임의 파기 시각이 지났거나 데이터 키를 지운 경우다. 갈래별 판단은 ParticipantQueryServiceTest가 검증한다.
        @Nested
        @DisplayName("개인 링크 토큰으로 참여자를 찾지 못한 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 모임을 읽지 않는다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                given(getLinkedParticipantUseCase.getLinkedParticipant(LINK_TOKEN))
                        .willThrow(new LinkTokenNotFoundException());

                // when & then
                assertThatThrownBy(() -> settlementQueryService.getForm(LINK_TOKEN))
                        .isInstanceOf(LinkTokenNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 남은 경우")
        class WhenGatheringRetained {

            @Test
            @DisplayName("모임의 파기 시각을 함께 반환한다")
            void shouldReturnDeletesAt() {
                // given
                givenForm(OPEN_PERIOD_OPENED_AT, null, false, List.of());

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.deletesAt()).isEqualTo(DELETES_AT);
            }
        }

        @Nested
        @DisplayName("모임의 첫 정산 건을 기록하는 경우")
        class WhenFirstSettlement {

            @Test
            @DisplayName("마스킹한 휴대폰을 담은 참여자 전원을 인원 기본값으로 하고 기록 가능, 계좌 미등록, 현재 계좌 동의 버전을 반환한다")
            void shouldReturnAllParticipantsAsDefault() {
                // given
                givenForm(OPEN_PERIOD_OPENED_AT, null, false, List.of());

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
        @DisplayName("모임에 정산 건이 없는 빈 차수가 있는 경우")
        class WhenEmptyRoundExists {

            @Test
            @DisplayName("빈 차수를 포함한 모임의 차수를 번호 순서대로 모두 반환한다")
            void shouldReturnRoundsOfGathering() {
                // given
                givenForm(OPEN_PERIOD_OPENED_AT, null, false, List.of());

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.rounds())
                        .extracting(GatheringRoundResult::id, GatheringRoundResult::number)
                        .containsExactly(
                                tuple(
                                        GatheringRoundFixture.FIRST.getId(),
                                        GatheringRoundFixture.FIRST.getNumber()),
                                tuple(
                                        GatheringRoundFixture.SECOND.getId(),
                                        GatheringRoundFixture.SECOND.getNumber()));
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
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                LEE_ID,
                                List.of(PAYER_ID, LEE_ID));
                givenForm(OPEN_PERIOD_OPENED_AT, latest, false, List.of());

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.defaultParticipantIds()).containsExactly(LEE_ID, PAYER_ID);
            }
        }

        @Nested
        @DisplayName("직전 정산 건이 이전 차수보다 적은 인원으로 기록된 경우")
        class WhenLatestSettlementHasFewerParticipants {

            @Test
            @DisplayName("차수와 관계없이 직전 정산 건의 인원만 인원 기본값으로 반환하고 참여자는 전원을 반환한다")
            void shouldReturnLatestSettlementParticipantsAsDefaultRegardlessOfRound() {
                // given
                List<Long> gatheringParticipantIds =
                        LongStream.range(PAYER_ID, PAYER_ID + 10).boxed().toList();
                List<Long> latestParticipantIds =
                        List.of(
                                gatheringParticipantIds.get(0),
                                gatheringParticipantIds.get(3),
                                gatheringParticipantIds.get(5),
                                gatheringParticipantIds.get(8));
                Settlement latest =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                GatheringRoundFixture.SECOND.getId(),
                                PAYER_ID,
                                latestParticipantIds);
                givenForm(
                        gathering(
                                OPEN_PERIOD_OPENED_AT,
                                ParticipantFixture.createWithIds(gatheringParticipantIds)),
                        latest,
                        false,
                        List.of());

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.defaultParticipantIds())
                        .containsExactlyElementsOf(latestParticipantIds);
                assertThat(result.participants())
                        .extracting(SettlementParticipantResult::id)
                        .containsExactlyElementsOf(gatheringParticipantIds);
            }
        }

        @Nested
        @DisplayName("요청한 참여자가 결제자로 기록한 정산 건이 있는 경우")
        class WhenPayerSettlementsExist {

            @Test
            @DisplayName("계좌 등록 여부와 함께 그 정산 건을 분담금과 함께 반환한다")
            void shouldReturnPayerSettlements() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID, PARK_ID));
                givenForm(OPEN_PERIOD_OPENED_AT, settlement, true, List.of(settlement));

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

            @Test
            @DisplayName("한 결제자가 여러 차수에서 결제한 정산 건의 총액과 받을 금액을 각각 더해 반환한다")
            void shouldSumTotalAndReceivableAmountsAcrossRounds() {
                // given
                Settlement firstRoundSettlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                GatheringRoundFixture.FIRST.getId(),
                                PAYER_ID,
                                PARTICIPANT_IDS);
                Settlement secondRoundSettlement =
                        SettlementFixture.SECOND_ROUND.createWithId(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                GatheringRoundFixture.SECOND.getId(),
                                PAYER_ID,
                                PARTICIPANT_IDS);
                givenForm(
                        OPEN_PERIOD_OPENED_AT,
                        secondRoundSettlement,
                        true,
                        List.of(firstRoundSettlement, secondRoundSettlement));

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.totalAmountSum()).isEqualTo(30_000L);
                assertThat(result.receivableAmountSum()).isEqualTo(19_998L);
            }

            @Test
            @DisplayName("정산 건마다 총액에서 결제자 분담액을 뺀 받을 금액을 담아 반환한다")
            void shouldReturnReceivableAmountOfSettlement() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS);
                givenForm(OPEN_PERIOD_OPENED_AT, settlement, true, List.of(settlement));

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.settlements())
                        .extracting(
                                SettlementResult::payerAmount, SettlementResult::receivableAmount)
                        .containsExactly(tuple(3_334L, 6_666L));
            }

            @Test
            @DisplayName("정산 건이 속한 차수의 id와 번호를 담아 반환한다")
            void shouldReturnRoundOfSettlement() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                GatheringRoundFixture.SECOND.getId(),
                                PAYER_ID,
                                PARTICIPANT_IDS);
                givenForm(OPEN_PERIOD_OPENED_AT, settlement, true, List.of(settlement));

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.settlements())
                        .extracting(SettlementResult::roundId, SettlementResult::roundNumber)
                        .containsExactly(
                                tuple(
                                        GatheringRoundFixture.SECOND.getId(),
                                        GatheringRoundFixture.SECOND.getNumber()));
            }
        }

        @Nested
        @DisplayName("요청한 참여자가 결제자로 기록한 정산 건이 없는 경우")
        class WhenNoPayerSettlements {

            @Test
            @DisplayName("총액과 받을 금액의 합계를 0으로 반환한다")
            void shouldReturnZeroSums() {
                // given
                givenForm(OPEN_PERIOD_OPENED_AT, null, true, List.of());

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.totalAmountSum()).isZero();
                assertThat(result.receivableAmountSum()).isZero();
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("기록 불가로 표시한다")
            void shouldReturnNotRecordableForm() {
                // given
                givenForm(CLOSED_PERIOD_OPENED_AT, null, true, List.of());

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.recordable()).isFalse();
            }
        }

        // 모임에 정산 건이 없는 경우는 latestSettlement에 null을 넘긴다.
        private void givenForm(
                LocalDateTime openedAt,
                Settlement latestSettlement,
                boolean bankAccountRegistered,
                List<Settlement> payerSettlements) {
            givenForm(
                    gathering(openedAt), latestSettlement, bankAccountRegistered, payerSettlements);
        }

        private void givenForm(
                Gathering gathering,
                Settlement latestSettlement,
                boolean bankAccountRegistered,
                List<Settlement> payerSettlements) {
            givenParticipant(PAYER_ID);
            given(gatheringRepository.findByParticipantId(PAYER_ID))
                    .willReturn(Optional.of(gathering));
            given(settlementRepository.findLatestByGatheringId(GATHERING_ID))
                    .willReturn(Optional.ofNullable(latestSettlement));
            given(participantBankAccountRepository.existsByParticipantId(PAYER_ID))
                    .willReturn(bankAccountRegistered);
            given(settlementRepository.findAllByPayerParticipantId(PAYER_ID))
                    .willReturn(payerSettlements);
        }
    }

    @Nested
    @DisplayName("getShares 메서드는")
    class GetShares {

        // 토큰이 없거나 모임의 파기 시각이 지났거나 데이터 키를 지운 경우다. 갈래별 판단은 ParticipantQueryServiceTest가 검증한다.
        @Nested
        @DisplayName("개인 링크 토큰으로 참여자를 찾지 못한 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 모임을 읽지 않는다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                given(getLinkedParticipantUseCase.getLinkedParticipant(LINK_TOKEN))
                        .willThrow(new LinkTokenNotFoundException());

                // when & then
                assertThatThrownBy(() -> settlementQueryService.getShares(LINK_TOKEN))
                        .isInstanceOf(LinkTokenNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 남은 경우")
        class WhenGatheringRetained {

            @Test
            @DisplayName("결제자별 분담금과 함께 모임의 파기 시각을 반환한다")
            void shouldReturnDeletesAt() {
                // given
                givenShares(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS));

                // when
                SharesResult result = settlementQueryService.getShares(LINK_TOKEN);

                // then
                assertThat(result.deletesAt()).isEqualTo(DELETES_AT);
                assertThat(result.payers())
                        .extracting(PayerSharesResult::payerParticipantId)
                        .containsExactly(PAYER_ID);
            }
        }

        @Nested
        @DisplayName("낼 분담금이 없는 경우")
        class WhenSharesNotFound {

            @Test
            @DisplayName("빈 목록을 반환한다")
            void shouldReturnEmptyListWhenSharesNotFound() {
                // given
                givenParticipant(LEE_ID);
                given(gatheringRepository.findByParticipantId(LEE_ID))
                        .willReturn(Optional.of(gathering(OPEN_PERIOD_OPENED_AT)));
                given(settlementRepository.findAllByShareParticipantId(LEE_ID))
                        .willReturn(List.of());

                // when
                List<PayerSharesResult> result =
                        settlementQueryService.getShares(LINK_TOKEN).payers();

                // then
                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("여러 결제자에게 낼 분담금이 있는 경우")
        class WhenSharesOfPayersExist {

            @Test
            @DisplayName("결제자별로 묶어 결제자 이름과 계좌를 함께 반환한다")
            void shouldGroupSharesByPayer() {
                // given
                givenShares(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS),
                        SettlementFixture.SECOND_ROUND.createWithId(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PARK_ID,
                                PARTICIPANT_IDS),
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                THIRD_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID),
                                ShareStatus.SENT));

                // when
                List<PayerSharesResult> result =
                        settlementQueryService.getShares(LINK_TOKEN).payers();

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
            @DisplayName("미입금 분담금이 남은 결제자를 먼저 기록한 결제자보다 앞에 둔다")
            void shouldPutPayersWithUnpaidSharesFirst() {
                // given
                givenShares(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT),
                        SettlementFixture.SECOND_ROUND.createWithId(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PARK_ID,
                                PARTICIPANT_IDS));

                // when
                List<PayerSharesResult> result =
                        settlementQueryService.getShares(LINK_TOKEN).payers();

                // then
                assertThat(result)
                        .extracting(
                                PayerSharesResult::payerParticipantId,
                                PayerSharesResult::hasUnpaidShares)
                        .containsExactly(tuple(PARK_ID, true), tuple(PAYER_ID, false));
            }

            @Test
            @DisplayName("0원으로 지정해 확인 완료로 시작한 분담금만 있는 결제자는 낼 분담금이 없어 뒤에 둔다")
            void shouldPutPayerWithOnlyZeroShareLast() {
                // given
                givenShares(
                        SettlementFixture.DINNER.createWithFixedAmounts(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                Map.of(LEE_ID, 0L)),
                        SettlementFixture.SECOND_ROUND.createWithId(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PARK_ID,
                                PARTICIPANT_IDS));

                // when
                List<PayerSharesResult> result =
                        settlementQueryService.getShares(LINK_TOKEN).payers();

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
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS),
                        SettlementFixture.SECOND_ROUND.createWithId(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PARK_ID,
                                PARTICIPANT_IDS));

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
        @DisplayName("상태가 섞인 분담금이 있는 경우")
        class WhenSharesHaveMixedStatuses {

            @Test
            @DisplayName("미입금 분담금만 더해 미입금 금액으로 반환한다")
            void shouldSumOnlyUnpaidShares() {
                // given
                givenShares(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS),
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID),
                                ShareStatus.SENT),
                        SettlementFixture.SECOND_ROUND.createWithFirstShareStatus(
                                THIRD_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.CONFIRMED));

                // when
                List<PayerSharesResult> result =
                        settlementQueryService.getShares(LINK_TOKEN).payers();

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
        @DisplayName("같은 차수에 다른 결제자의 정산 건이 있는 경우")
        class WhenRoundHasOtherSettlements {

            @Test
            @DisplayName("본인이 인원이 아닌 정산 건까지 합쳐 그 차수 참여자의 이름을 참여한 순서대로 반환한다")
            void shouldReturnUnionOfRoundParticipantNames() {
                // given
                Settlement mySettlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID));
                Settlement otherSettlement =
                        SettlementFixture.SECOND_ROUND.createWithId(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PARK_ID,
                                List.of(PARK_ID, PAYER_ID));
                givenShares(List.of(mySettlement), List.of(otherSettlement, mySettlement));

                // when
                List<PayerSharesResult> result =
                        settlementQueryService.getShares(LINK_TOKEN).payers();

                // then
                assertThat(result.getFirst().shares())
                        .singleElement()
                        .extracting(ShareBreakdownResult::roundParticipantNames)
                        .isEqualTo(
                                List.of(
                                        ParticipantFixture.KIM.getName(),
                                        ParticipantFixture.LEE.getName(),
                                        ParticipantFixture.PARK.getName()));
            }

            @Test
            @DisplayName("취소한 정산 건에만 있는 참여자의 이름은 담지 않는다")
            void shouldExcludeParticipantsOfCancelledSettlements() {
                // given
                Settlement mySettlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID));
                Settlement cancelledSettlement =
                        SettlementFixture.SECOND_ROUND.createCancelled(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PARK_ID,
                                List.of(PARK_ID, PAYER_ID));
                givenShares(List.of(mySettlement), List.of(cancelledSettlement, mySettlement));

                // when
                List<PayerSharesResult> result =
                        settlementQueryService.getShares(LINK_TOKEN).payers();

                // then
                assertThat(result.getFirst().shares())
                        .singleElement()
                        .extracting(ShareBreakdownResult::roundParticipantNames)
                        .isEqualTo(
                                List.of(
                                        ParticipantFixture.KIM.getName(),
                                        ParticipantFixture.LEE.getName()));
            }

            @Test
            @DisplayName("모임에 참여하지 않은 id가 정산 건 인원에 있으면 건너뛰고 나머지 이름만 반환한다")
            void shouldSkipParticipantIdsNotInGathering() {
                // given
                Settlement mySettlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID));
                Settlement settlementOfNewcomer =
                        SettlementFixture.SECOND_ROUND.createWithId(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PARK_ID,
                                List.of(PARK_ID, NEWCOMER_ID));
                givenShares(List.of(mySettlement), List.of(settlementOfNewcomer, mySettlement));

                // when
                List<PayerSharesResult> result =
                        settlementQueryService.getShares(LINK_TOKEN).payers();

                // then
                assertThat(result.getFirst().shares())
                        .singleElement()
                        .extracting(ShareBreakdownResult::roundParticipantNames)
                        .isEqualTo(
                                List.of(
                                        ParticipantFixture.KIM.getName(),
                                        ParticipantFixture.LEE.getName(),
                                        ParticipantFixture.PARK.getName()));
            }

            @Test
            @DisplayName("다른 차수의 정산 건 인원은 섞지 않고 차수마다 따로 반환한다")
            void shouldReturnParticipantNamesPerRound() {
                // given
                Settlement firstRoundSettlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                GatheringRoundFixture.FIRST.getId(),
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID));
                Settlement secondRoundSettlement =
                        SettlementFixture.SECOND_ROUND.createWithId(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                GatheringRoundFixture.SECOND.getId(),
                                PARK_ID,
                                List.of(PARK_ID, LEE_ID));
                givenShares(firstRoundSettlement, secondRoundSettlement);

                // when
                List<PayerSharesResult> result =
                        settlementQueryService.getShares(LINK_TOKEN).payers();

                // then
                assertThat(result)
                        .flatExtracting(PayerSharesResult::shares)
                        .extracting(
                                ShareBreakdownResult::roundNumber,
                                ShareBreakdownResult::roundParticipantNames)
                        .containsExactlyInAnyOrder(
                                tuple(
                                        1,
                                        List.of(
                                                ParticipantFixture.KIM.getName(),
                                                ParticipantFixture.LEE.getName())),
                                tuple(
                                        2,
                                        List.of(
                                                ParticipantFixture.LEE.getName(),
                                                ParticipantFixture.PARK.getName())));
            }
        }

        @Nested
        @DisplayName("결제자가 금액을 지정한 정산 건인 경우")
        class WhenAmountFixed {

            @Test
            @DisplayName("다른 참여자의 금액 없이 본인 분담금과 총액, 인원, 지정 여부, 분담금 상태를 계산 근거로 반환한다")
            void shouldReturnOwnShareBreakdown() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.createWithFixedAmounts(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                Map.of(LEE_ID, 1_050L));
                givenShares(settlement);

                // when
                List<PayerSharesResult> result =
                        settlementQueryService.getShares(LINK_TOKEN).payers();

                // then
                assertThat(result.getFirst().shares())
                        .singleElement()
                        .isEqualTo(
                                new ShareBreakdownResult(
                                        SETTLEMENT_ID,
                                        ROUND_ID,
                                        GatheringRoundFixture.FIRST.getNumber(),
                                        List.of(
                                                ParticipantFixture.KIM.getName(),
                                                ParticipantFixture.LEE.getName(),
                                                ParticipantFixture.PARK.getName()),
                                        SettlementFixture.DINNER.getTotalAmount(),
                                        3,
                                        true,
                                        1_050L,
                                        ShareStatus.UNPAID,
                                        SettlementFixture.DINNER.getRecordedAt()));
            }
        }

        private void givenShares(Settlement... settlements) {
            givenShares(List.of(settlements), List.of(settlements));
        }

        // 모임의 정산 건에는 LEE가 분담금을 내지 않는 정산 건과 취소한 정산 건도 들어 있을 수 있다.
        private void givenShares(
                List<Settlement> settlements, List<Settlement> gatheringSettlements) {
            givenParticipant(LEE_ID);
            given(gatheringRepository.findByParticipantId(LEE_ID))
                    .willReturn(Optional.of(gathering(OPEN_PERIOD_OPENED_AT)));
            given(settlementRepository.findAllByShareParticipantId(LEE_ID)).willReturn(settlements);
            given(settlementRepository.findAllByGatheringId(GATHERING_ID))
                    .willReturn(gatheringSettlements);
            given(participantBankAccountRepository.findAllByParticipantIds(any(), eq(GATHERING_ID)))
                    .willReturn(
                            List.of(
                                    ParticipantBankAccountFixture.KOOKMIN.create(PAYER_ID),
                                    ParticipantBankAccountFixture.KOOKMIN.create(PARK_ID)));
        }
    }

    private void givenParticipant(Long participantId) {
        given(getLinkedParticipantUseCase.getLinkedParticipant(LINK_TOKEN))
                .willReturn(new LinkedParticipantResult(participantId, DELETES_AT));
    }

    // 1차에만 정산 건이 있고 2차는 비어 있는 모임이다.
    private Gathering gathering(LocalDateTime openedAt) {
        return gathering(openedAt, ParticipantFixture.createWithIds(PARTICIPANT_IDS));
    }

    private Gathering gathering(LocalDateTime openedAt, List<Participant> participants) {
        return GatheringFixture.FRIDAY_DINNER.createWithRounds(
                GATHERING_ID,
                openedAt,
                participants,
                List.of(
                        GatheringRoundFixture.FIRST.create(),
                        GatheringRoundFixture.SECOND.create()));
    }
}
