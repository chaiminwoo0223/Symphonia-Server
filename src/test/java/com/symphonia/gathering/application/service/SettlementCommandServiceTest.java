package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.command.CancelSettlementCommand;
import com.symphonia.gathering.application.dto.command.RecordSettlementCommand;
import com.symphonia.gathering.application.dto.command.RegisterBankAccountCommand;
import com.symphonia.gathering.application.dto.command.UpdateSettlementCommand;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.dto.result.SettlementShareResult;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.ParticipantBankAccount;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.BankAccountAlreadyRegisteredException;
import com.symphonia.gathering.domain.exception.BankAccountConsentRequiredException;
import com.symphonia.gathering.domain.exception.BankAccountRequiredException;
import com.symphonia.gathering.domain.exception.FixedAmountExceedsTotalException;
import com.symphonia.gathering.domain.exception.InvalidBankAccountException;
import com.symphonia.gathering.domain.exception.InvalidSettlementAmountException;
import com.symphonia.gathering.domain.exception.InvalidSettlementParticipantsException;
import com.symphonia.gathering.domain.exception.NotSettlementPayerException;
import com.symphonia.gathering.domain.exception.OutdatedBankAccountConsentVersionException;
import com.symphonia.gathering.domain.exception.ParticipantNotFoundException;
import com.symphonia.gathering.domain.exception.SettlementAlreadyCancelledException;
import com.symphonia.gathering.domain.exception.SettlementAlreadyNotifiedException;
import com.symphonia.gathering.domain.exception.SettlementNotFoundException;
import com.symphonia.gathering.domain.exception.SettlementPaymentStartedException;
import com.symphonia.gathering.domain.exception.SettlementRecordingClosedException;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.BankAccount;
import com.symphonia.gathering.domain.vo.PaymentStatus;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantBankAccountFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("SettlementCommandService 단위 테스트")
class SettlementCommandServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long SETTLEMENT_ID = 30L;
    private static final Long PAYER_ID = 20L;
    private static final Long LEE_ID = 21L;
    private static final Long PARK_ID = 22L;
    private static final Long OUTSIDER_ID = 99L;
    private static final List<Long> PARTICIPANT_IDS = List.of(PAYER_ID, LEE_ID, PARK_ID);
    private static final String LINK_TOKEN = "link-token";
    private static final String HYPHENATED_ACCOUNT_NUMBER = "1234-5678-9012";

    @InjectMocks private SettlementCommandService settlementCommandService;

    @Mock private ParticipantLinkTokenRepository participantLinkTokenRepository;
    @Mock private GatheringRepository gatheringRepository;
    @Mock private ParticipantBankAccountRepository participantBankAccountRepository;
    @Mock private SettlementRepository settlementRepository;

    @Nested
    @DisplayName("register 메서드는")
    class Register {

        @Nested
        @DisplayName("개인 링크 토큰에 해당하는 참여자가 없는 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 계좌를 저장하지 않는다")
            void shouldThrowParticipantNotFoundException() {
                // given
                given(participantLinkTokenRepository.findParticipantIdByValue(LINK_TOKEN))
                        .willReturn(Optional.empty());
                RegisterBankAccountCommand command = command(true);

                // when & then
                assertThatThrownBy(() -> settlementCommandService.register(command))
                        .isInstanceOf(ParticipantNotFoundException.class);
                thenBankAccountNotSaved();
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 지난 경우")
        class WhenRecordingPeriodPassed {

            @Test
            @DisplayName("예외를 던지고 계좌를 저장하지 않는다")
            void shouldThrowSettlementRecordingClosedException() {
                // given
                givenGatheringOfPayer(LocalDateTime.now().minusHours(49));
                RegisterBankAccountCommand command = command(true);

                // when & then
                assertThatThrownBy(() -> settlementCommandService.register(command))
                        .isInstanceOf(SettlementRecordingClosedException.class);
                thenBankAccountNotSaved();
            }
        }

        @Nested
        @DisplayName("계좌를 보여 주는 데 동의하지 않은 경우")
        class WhenConsentNotAgreed {

            @Test
            @DisplayName("예외를 던지고 계좌를 저장하지 않는다")
            void shouldThrowBankAccountConsentRequiredException() {
                // given
                givenGatheringOfPayer(LocalDateTime.now().minusHours(1));
                RegisterBankAccountCommand command = command(false);

                // when & then
                assertThatThrownBy(() -> settlementCommandService.register(command))
                        .isInstanceOf(BankAccountConsentRequiredException.class);
                thenBankAccountNotSaved();
            }
        }

        @Nested
        @DisplayName("계좌 동의 안내 버전이 현재 버전과 다른 경우")
        class WhenConsentVersionOutdated {

            @Test
            @DisplayName("예외를 던지고 계좌를 저장하지 않는다")
            void shouldThrowOutdatedBankAccountConsentVersionException() {
                // given
                givenGatheringOfPayer(LocalDateTime.now().minusHours(1));
                RegisterBankAccountCommand command =
                        new RegisterBankAccountCommand(
                                LINK_TOKEN,
                                ParticipantBankAccountFixture.KOOKMIN.getBankName(),
                                HYPHENATED_ACCOUNT_NUMBER,
                                true,
                                "2000-01-01");

                // when & then
                assertThatThrownBy(() -> settlementCommandService.register(command))
                        .isInstanceOf(OutdatedBankAccountConsentVersionException.class);
                thenBankAccountNotSaved();
            }
        }

        @Nested
        @DisplayName("이미 계좌를 등록한 참여자인 경우")
        class WhenBankAccountAlreadyRegistered {

            @Test
            @DisplayName("예외를 던지고 계좌를 다시 저장하지 않는다")
            void shouldThrowBankAccountAlreadyRegisteredException() {
                // given
                givenGatheringOfPayer(LocalDateTime.now().minusHours(1));
                given(participantBankAccountRepository.existsByParticipantId(PAYER_ID))
                        .willReturn(true);
                RegisterBankAccountCommand command = command(true);

                // when & then
                assertThatThrownBy(() -> settlementCommandService.register(command))
                        .isInstanceOf(BankAccountAlreadyRegisteredException.class);
                thenBankAccountNotSaved();
            }
        }

        @Nested
        @DisplayName("계좌번호 형식이 틀린 경우")
        class WhenAccountNumberInvalid {

            @Test
            @DisplayName("예외를 던지고 계좌를 저장하지 않는다")
            void shouldThrowInvalidBankAccountException() {
                // given
                givenGatheringOfPayer(LocalDateTime.now().minusHours(1));
                RegisterBankAccountCommand command =
                        new RegisterBankAccountCommand(
                                LINK_TOKEN,
                                ParticipantBankAccountFixture.KOOKMIN.getBankName(),
                                "12345",
                                true,
                                SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> settlementCommandService.register(command))
                        .isInstanceOf(InvalidBankAccountException.class);
                thenBankAccountNotSaved();
            }
        }

        @Nested
        @DisplayName("등록할 수 있는 경우")
        class WhenRegistrable {

            @Test
            @DisplayName("하이픈을 지운 계좌와 동의 버전을 참여자가 속한 모임 id와 함께 저장한다")
            void shouldSaveNormalizedBankAccountWithGatheringId() {
                // given
                givenGatheringOfPayer(LocalDateTime.now().minusHours(1));
                given(participantBankAccountRepository.existsByParticipantId(PAYER_ID))
                        .willReturn(false);
                RegisterBankAccountCommand command = command(true);

                // when
                settlementCommandService.register(command);

                // then
                ArgumentCaptor<ParticipantBankAccount> captor =
                        ArgumentCaptor.forClass(ParticipantBankAccount.class);
                then(participantBankAccountRepository)
                        .should()
                        .save(captor.capture(), eq(GATHERING_ID));
                ParticipantBankAccount saved = captor.getValue();
                assertThat(saved.getParticipantId()).isEqualTo(PAYER_ID);
                assertThat(saved.getBankAccount())
                        .isEqualTo(
                                new BankAccount(
                                        ParticipantBankAccountFixture.KOOKMIN.getBankName(),
                                        ParticipantBankAccountFixture.KOOKMIN.getAccountNumber()));
                assertThat(saved.getConsentVersion())
                        .isEqualTo(SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION);
            }
        }

        private RegisterBankAccountCommand command(boolean consentAgreed) {
            return new RegisterBankAccountCommand(
                    LINK_TOKEN,
                    ParticipantBankAccountFixture.KOOKMIN.getBankName(),
                    HYPHENATED_ACCOUNT_NUMBER,
                    consentAgreed,
                    SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION);
        }

        private void thenBankAccountNotSaved() {
            then(participantBankAccountRepository).should(never()).save(any(), anyLong());
        }
    }

    @Nested
    @DisplayName("record 메서드는")
    class Record {

        @Nested
        @DisplayName("모임을 만든 지 48시간이 지난 경우")
        class WhenRecordingPeriodPassed {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementRecordingClosedException() {
                // given
                givenGatheringOfPayer(LocalDateTime.now().minusHours(49));
                RecordSettlementCommand command = command(10_000L, PARTICIPANT_IDS, Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(SettlementRecordingClosedException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("인원에 이 모임의 참여자가 아닌 id가 있는 경우")
        class WhenParticipantNotInGathering {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowInvalidSettlementParticipantsException() {
                // given
                givenGatheringOfPayer(LocalDateTime.now().minusHours(1));
                RecordSettlementCommand command =
                        command(10_000L, List.of(PAYER_ID, OUTSIDER_ID), Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(InvalidSettlementParticipantsException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("결제자가 계좌를 등록하지 않은 경우")
        class WhenBankAccountNotRegistered {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowBankAccountRequiredException() {
                // given
                givenGatheringOfPayer(LocalDateTime.now().minusHours(1));
                given(participantBankAccountRepository.existsByParticipantId(PAYER_ID))
                        .willReturn(false);
                RecordSettlementCommand command = command(10_000L, PARTICIPANT_IDS, Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(BankAccountRequiredException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("총액이나 지정 금액이 범위를 벗어난 경우")
        class WhenAmountInvalid {

            @Test
            @DisplayName("총액이 0 이하면 예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowInvalidSettlementAmountExceptionWhenTotalAmountNotPositive() {
                // given
                givenRecordableGathering();
                RecordSettlementCommand command = command(0L, PARTICIPANT_IDS, Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(InvalidSettlementAmountException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }

            @Test
            @DisplayName("지정 금액이 음수면 예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowInvalidSettlementAmountExceptionWhenFixedAmountNegative() {
                // given
                givenRecordableGathering();
                RecordSettlementCommand command =
                        command(10_000L, PARTICIPANT_IDS, Map.of(LEE_ID, -1L));

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(InvalidSettlementAmountException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("지정 금액의 합이 총액보다 큰 경우")
        class WhenFixedAmountsExceedTotal {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowFixedAmountExceedsTotalException() {
                // given
                givenRecordableGathering();
                RecordSettlementCommand command =
                        command(10_000L, PARTICIPANT_IDS, Map.of(LEE_ID, 6_000L, PARK_ID, 5_000L));

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(FixedAmountExceedsTotalException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }

            @Test
            @DisplayName("합이 long 범위를 넘어도 음수로 돌아오지 않고 예외를 던진다")
            void shouldThrowFixedAmountExceedsTotalExceptionWhenSumOverflows() {
                // given
                givenRecordableGathering();
                RecordSettlementCommand command =
                        command(
                                10_000L,
                                PARTICIPANT_IDS,
                                Map.of(LEE_ID, Long.MAX_VALUE, PARK_ID, Long.MAX_VALUE));

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(FixedAmountExceedsTotalException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("인원이나 금액 지정 대상이 분담 방식의 규칙에 맞지 않는 경우")
        class WhenParticipantsInvalid {

            @Test
            @DisplayName("인원이 2명 미만이면 예외를 던진다")
            void shouldThrowInvalidSettlementParticipantsExceptionWhenFewerThanTwo() {
                // given
                givenRecordableGathering();
                RecordSettlementCommand command = command(10_000L, List.of(PAYER_ID), Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(InvalidSettlementParticipantsException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }

            @Test
            @DisplayName("인원에 결제자가 없으면 예외를 던진다")
            void shouldThrowInvalidSettlementParticipantsExceptionWhenPayerExcluded() {
                // given
                givenRecordableGathering();
                RecordSettlementCommand command =
                        command(10_000L, List.of(LEE_ID, PARK_ID), Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(InvalidSettlementParticipantsException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }

            @Test
            @DisplayName("결제자의 금액을 지정하면 예외를 던진다")
            void shouldThrowInvalidSettlementParticipantsExceptionWhenPayerAmountFixed() {
                // given
                givenRecordableGathering();
                RecordSettlementCommand command =
                        command(10_000L, PARTICIPANT_IDS, Map.of(PAYER_ID, 1_000L));

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(InvalidSettlementParticipantsException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }

            @Test
            @DisplayName("인원에 없는 참여자의 금액을 지정하면 예외를 던진다")
            void shouldThrowInvalidSettlementParticipantsExceptionWhenFixedParticipantExcluded() {
                // given
                givenRecordableGathering();
                RecordSettlementCommand command =
                        command(10_000L, List.of(PAYER_ID, LEE_ID), Map.of(PARK_ID, 1_000L));

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(InvalidSettlementParticipantsException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }

            @Test
            @DisplayName("인원에 같은 id가 두 번 있으면 예외를 던진다")
            void shouldThrowInvalidSettlementParticipantsExceptionWhenParticipantIdsDuplicated() {
                // given
                givenRecordableGathering();
                RecordSettlementCommand command =
                        command(10_000L, List.of(PAYER_ID, LEE_ID, LEE_ID), Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(InvalidSettlementParticipantsException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("기록할 수 있는 경우")
        class WhenRecordable {

            @Test
            @DisplayName("결제자를 뺀 참여자의 분담을 미입금으로 계산해 저장하고 반환한다")
            void shouldSaveSettlementWithCalculatedShares() {
                // given
                givenRecordableGathering();
                givenSaveReturnsSettlement();
                RecordSettlementCommand command = command(10_000L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
                then(settlementRepository).should().save(captor.capture());
                assertThat(captor.getValue().getGatheringId()).isEqualTo(GATHERING_ID);
                assertThat(result.payerParticipantId()).isEqualTo(PAYER_ID);
                assertThat(result.totalAmount()).isEqualTo(10_000L);
                assertThat(result.notified()).isFalse();
                assertThat(result.shares())
                        .extracting(
                                SettlementShareResult::participantId,
                                SettlementShareResult::amount,
                                SettlementShareResult::fixed,
                                SettlementShareResult::paymentStatus)
                        .containsExactly(
                                tuple(LEE_ID, 3_400L, false, PaymentStatus.UNPAID),
                                tuple(PARK_ID, 3_400L, false, PaymentStatus.UNPAID));
                assertThat(result.payerAmount()).isEqualTo(3_200L);
            }
        }

        @Nested
        @DisplayName("분담액을 100원 단위로 맞추는 경우")
        class WhenSharesRounded {

            @Test
            @DisplayName("몫을 100원 단위로 올리고 나머지는 결제자에게 귀속한다")
            void shouldRoundEqualShareUpAndAttributeRemainderToPayer() {
                // given
                givenRecordableGathering();
                givenSaveReturnsSettlement();
                RecordSettlementCommand command = command(10_000L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.shares())
                        .extracting(SettlementShareResult::amount)
                        .containsExactly(3_400L, 3_400L);
                assertThat(result.payerAmount()).isEqualTo(3_200L);
            }

            @Test
            @DisplayName("남은 금액이 100원 단위보다 작게 남아도 올린 몫의 합이 넘지 않으면 올린다")
            void shouldRoundUpWhenRoundedUpSharesFitRemainingAmount() {
                // given
                givenRecordableGathering();
                givenSaveReturnsSettlement();
                RecordSettlementCommand command = command(250L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.shares())
                        .extracting(SettlementShareResult::amount)
                        .containsExactly(100L, 100L);
                assertThat(result.payerAmount()).isEqualTo(50L);
            }

            @Test
            @DisplayName("올린 몫의 합이 남은 금액을 넘으면 내린 몫으로 바꿔 결제자가 총액보다 많이 돌려받지 않게 한다")
            void shouldRoundDownWhenRoundedUpSharesExceedRemainingAmount() {
                // given
                givenRecordableGathering();
                givenSaveReturnsSettlement();
                RecordSettlementCommand command = command(100L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.shares())
                        .extracting(SettlementShareResult::amount)
                        .containsExactly(0L, 0L);
                assertThat(result.payerAmount()).isEqualTo(100L);
            }

            @Test
            @DisplayName("지정 금액은 올리지 않고, 지정하지 않은 참여자가 결제자와 남은 금액을 나눈다")
            void shouldKeepFixedAmountUnrounded() {
                // given
                givenRecordableGathering();
                givenSaveReturnsSettlement();
                RecordSettlementCommand command =
                        command(10_000L, PARTICIPANT_IDS, Map.of(LEE_ID, 1_050L));

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.shares())
                        .extracting(
                                SettlementShareResult::participantId,
                                SettlementShareResult::amount,
                                SettlementShareResult::fixed)
                        .containsExactly(
                                tuple(LEE_ID, 1_050L, true), tuple(PARK_ID, 4_500L, false));
                assertThat(result.payerAmount()).isEqualTo(4_450L);
            }

            @Test
            @DisplayName("다른 참여자가 모두 지정 금액이면 결제자가 남은 금액 전부를 분담한다")
            void shouldAttributeRemainingAmountToPayerWhenOthersFixed() {
                // given
                givenRecordableGathering();
                givenSaveReturnsSettlement();
                RecordSettlementCommand command =
                        command(10_000L, PARTICIPANT_IDS, Map.of(LEE_ID, 3_333L, PARK_ID, 0L));

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.shares())
                        .extracting(SettlementShareResult::amount)
                        .containsExactly(3_333L, 0L);
                assertThat(result.payerAmount()).isEqualTo(6_667L);
            }
        }

        private RecordSettlementCommand command(
                long totalAmount, List<Long> participantIds, Map<Long, Long> fixedAmounts) {
            return new RecordSettlementCommand(
                    LINK_TOKEN, totalAmount, participantIds, fixedAmounts);
        }

        private void givenRecordableGathering() {
            givenGatheringOfPayer(LocalDateTime.now().minusHours(1));
            given(participantBankAccountRepository.existsByParticipantId(PAYER_ID))
                    .willReturn(true);
        }

        private void givenSaveReturnsSettlement() {
            given(settlementRepository.save(any(Settlement.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
        }
    }

    @Nested
    @DisplayName("update 메서드는")
    class Update {

        @Nested
        @DisplayName("정산 건이 없는 경우")
        class WhenSettlementNotFound {

            @Test
            @DisplayName("예외를 던진다")
            void shouldThrowSettlementNotFoundException() {
                // given
                givenParticipant(PAYER_ID);
                given(settlementRepository.findById(SETTLEMENT_ID)).willReturn(Optional.empty());
                UpdateSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(SettlementNotFoundException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("요청한 참여자가 결제자가 아닌 경우")
        class WhenParticipantIsNotPayer {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowNotSettlementPayerException() {
                // given
                givenParticipant(LEE_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));
                UpdateSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(NotSettlementPayerException.class);
                thenSettlementNotSaved();
                then(gatheringRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 지난 경우")
        class WhenRecordingPeriodPassed {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementRecordingClosedException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));
                givenGathering(LocalDateTime.now().minusHours(49));
                UpdateSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(SettlementRecordingClosedException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("정산 메시지를 보낸 정산 건인 경우")
        class WhenSettlementNotified {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementAlreadyNotifiedException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createNotified(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));
                givenGathering(LocalDateTime.now().minusHours(1));
                UpdateSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(SettlementAlreadyNotifiedException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("취소한 정산 건인 경우")
        class WhenSettlementCancelled {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementAlreadyCancelledException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createCancelled(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));
                givenGathering(LocalDateTime.now().minusHours(1));
                UpdateSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(SettlementAlreadyCancelledException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("수정할 수 있는 경우")
        class WhenUpdatable {

            @Test
            @DisplayName("분담을 다시 계산하고 기록 시각을 바꿔 저장한다")
            void shouldSaveRecalculatedSharesWithNewRecordedAt() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));
                givenGathering(LocalDateTime.now().minusHours(1));
                given(settlementRepository.save(any(Settlement.class)))
                        .willAnswer(invocation -> invocation.getArgument(0));
                UpdateSettlementCommand command = command();

                // when
                SettlementResult result = settlementCommandService.update(command);

                // then
                assertThat(result.id()).isEqualTo(SETTLEMENT_ID);
                assertThat(result.totalAmount()).isEqualTo(20_000L);
                assertThat(result.shares())
                        .extracting(
                                SettlementShareResult::participantId, SettlementShareResult::amount)
                        .containsExactly(tuple(LEE_ID, 10_000L));
                assertThat(result.payerAmount()).isEqualTo(10_000L);
                assertThat(result.recordedAt()).isAfter(SettlementFixture.DINNER.getRecordedAt());
            }
        }

        private UpdateSettlementCommand command() {
            return new UpdateSettlementCommand(
                    LINK_TOKEN, SETTLEMENT_ID, 20_000L, List.of(PAYER_ID, LEE_ID), Map.of());
        }

        private void givenGathering(LocalDateTime createdAt) {
            given(gatheringRepository.findById(GATHERING_ID))
                    .willReturn(Optional.of(gathering(createdAt)));
        }
    }

    @Nested
    @DisplayName("cancel 메서드는")
    class Cancel {

        @Nested
        @DisplayName("요청한 참여자가 결제자가 아닌 경우")
        class WhenParticipantIsNotPayer {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowNotSettlementPayerException() {
                // given
                givenParticipant(LEE_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));

                // when & then
                assertThatThrownBy(() -> settlementCommandService.cancel(command()))
                        .isInstanceOf(NotSettlementPayerException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("이미 취소한 정산 건인 경우")
        class WhenSettlementCancelled {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementAlreadyCancelledException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createCancelled(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));

                // when & then
                assertThatThrownBy(() -> settlementCommandService.cancel(command()))
                        .isInstanceOf(SettlementAlreadyCancelledException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("미입금이 아닌 분담이 있는 경우")
        class WhenPaymentStarted {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementPaymentStartedException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                PaymentStatus.SENT));

                // when & then
                assertThatThrownBy(() -> settlementCommandService.cancel(command()))
                        .isInstanceOf(SettlementPaymentStartedException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("취소할 수 있는 경우")
        class WhenCancellable {

            @Test
            @DisplayName("취소 시각을 채워 저장한다")
            void shouldSaveCancelledSettlement() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS);
                givenParticipant(PAYER_ID);
                givenSettlement(settlement);

                // when
                settlementCommandService.cancel(command());

                // then
                then(settlementRepository).should().save(settlement);
                assertThat(settlement.isCancelled()).isTrue();
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 지난 경우")
        class WhenRecordingPeriodPassed {

            @Test
            @DisplayName("기록 기간을 확인하지 않고 취소한다")
            void shouldCancelSettlementWithoutCheckingRecordingPeriod() {
                // given: 기록 기간과 관계없이 취소할 수 있어 모임을 조회하지 않는 상태
                Settlement settlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS);
                givenParticipant(PAYER_ID);
                givenSettlement(settlement);

                // when
                settlementCommandService.cancel(command());

                // then
                then(gatheringRepository).shouldHaveNoInteractions();
                then(settlementRepository).should().save(settlement);
                assertThat(settlement.isCancelled()).isTrue();
            }
        }

        private CancelSettlementCommand command() {
            return CancelSettlementCommand.of(LINK_TOKEN, SETTLEMENT_ID);
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

    private void givenParticipant(Long participantId) {
        given(participantLinkTokenRepository.findParticipantIdByValue(LINK_TOKEN))
                .willReturn(Optional.of(participantId));
    }

    private void givenGatheringOfPayer(LocalDateTime createdAt) {
        givenParticipant(PAYER_ID);
        given(gatheringRepository.findByParticipantId(PAYER_ID))
                .willReturn(Optional.of(gathering(createdAt)));
    }

    private void givenSettlement(Settlement settlement) {
        given(settlementRepository.findById(SETTLEMENT_ID)).willReturn(Optional.of(settlement));
    }

    private void thenSettlementNotSaved() {
        then(settlementRepository).should(never()).save(any(Settlement.class));
    }
}
