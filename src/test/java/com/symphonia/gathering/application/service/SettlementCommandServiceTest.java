package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.command.CancelSettlementCommand;
import com.symphonia.gathering.application.dto.command.ConfirmShareCommand;
import com.symphonia.gathering.application.dto.command.MarkSharesSentCommand;
import com.symphonia.gathering.application.dto.command.RecordSettlementCommand;
import com.symphonia.gathering.application.dto.command.RegisterBankAccountCommand;
import com.symphonia.gathering.application.dto.command.RevertShareCommand;
import com.symphonia.gathering.application.dto.command.UpdateSettlementCommand;
import com.symphonia.gathering.application.dto.result.PayerSharesResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.dto.result.SettlementShareResult;
import com.symphonia.gathering.application.dto.result.ShareBreakdownResult;
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
import com.symphonia.gathering.domain.exception.LinkTokenNotFoundException;
import com.symphonia.gathering.domain.exception.NotSettlementPayerException;
import com.symphonia.gathering.domain.exception.OutdatedBankAccountConsentVersionException;
import com.symphonia.gathering.domain.exception.SettlementAlreadyCancelledException;
import com.symphonia.gathering.domain.exception.SettlementAlreadyNotifiedException;
import com.symphonia.gathering.domain.exception.SettlementNotFoundException;
import com.symphonia.gathering.domain.exception.SettlementRecordingClosedException;
import com.symphonia.gathering.domain.exception.SettlementShareNotFoundException;
import com.symphonia.gathering.domain.exception.SettlementSharesSentException;
import com.symphonia.gathering.domain.exception.ShareAlreadyConfirmedException;
import com.symphonia.gathering.domain.exception.ShareAlreadyUnpaidException;
import com.symphonia.gathering.domain.exception.ZeroShareStatusUnchangeableException;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.BankAccount;
import com.symphonia.gathering.domain.vo.ShareStatus;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantBankAccountFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.global.config.TimeConfig;
import com.symphonia.notification.application.service.NotificationCommandService;
import com.symphonia.notification.application.service.NotificationQueryService;
import com.symphonia.notification.domain.entity.NotificationDelivery;
import com.symphonia.notification.domain.entity.NotificationSchedule;
import com.symphonia.notification.domain.repository.NotificationDeliveryRepository;
import com.symphonia.notification.domain.repository.NotificationScheduleRepository;
import com.symphonia.notification.domain.vo.DeliveryStatus;
import com.symphonia.notification.domain.vo.NotificationType;
import com.symphonia.notification.fixture.NotificationDeliveryFixture;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;

@DisplayName("SettlementCommandService 단위 테스트")
class SettlementCommandServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long SETTLEMENT_ID = 30L;
    private static final Long SECOND_SETTLEMENT_ID = 31L;
    private static final Long THIRD_SETTLEMENT_ID = 32L;
    private static final Long PAYER_ID = 20L;
    private static final Long LEE_ID = 21L;
    private static final Long PARK_ID = 22L;
    private static final Long OUTSIDER_ID = 99L;
    private static final List<Long> PARTICIPANT_IDS = List.of(PAYER_ID, LEE_ID, PARK_ID);
    private static final String LINK_TOKEN = "link-token";
    private static final String HYPHENATED_ACCOUNT_NUMBER = "1234-5678-9012";
    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 10, 28, 20, 30);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 18, 0);

    // 기록 기간은 모임을 만든 지 48시간이 되는 순간 닫힌다.
    // 닫히기 1초 전과 닫히는 순간을 경계로 쓴다.
    private static final LocalDateTime OPEN_PERIOD_OPENED_AT = NOW.minusHours(48).plusSeconds(1);
    private static final LocalDateTime CLOSED_PERIOD_OPENED_AT = NOW.minusHours(48);

    private SettlementCommandService settlementCommandService;

    @Mock private ParticipantLinkTokenRepository participantLinkTokenRepository;
    @Mock private GatheringRepository gatheringRepository;
    @Mock private ParticipantBankAccountRepository participantBankAccountRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private NotificationScheduleRepository notificationScheduleRepository;
    @Mock private NotificationDeliveryRepository notificationDeliveryRepository;

    // 시각은 NOW로 고정한다. 발송 시각을 따지는 테스트만 givenNow로 다른 시각을 고정한다.
    @BeforeEach
    void setUp() {
        givenNow(NOW);
    }

    @Nested
    @DisplayName("register 메서드는")
    class Register {

        @Nested
        @DisplayName("개인 링크 토큰에 해당하는 참여자가 없는 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 계좌를 저장하지 않는다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                given(participantLinkTokenRepository.findParticipantIdByValue(LINK_TOKEN))
                        .willReturn(Optional.empty());
                RegisterBankAccountCommand command = command(true);

                // when & then
                assertThatThrownBy(() -> settlementCommandService.register(command))
                        .isInstanceOf(LinkTokenNotFoundException.class);
                thenBankAccountNotSaved();
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("예외를 던지고 계좌를 저장하지 않는다")
            void shouldThrowSettlementRecordingClosedException() {
                // given
                givenGatheringOfPayer(CLOSED_PERIOD_OPENED_AT);
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
                givenGatheringOfPayer(OPEN_PERIOD_OPENED_AT);
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
                givenGatheringOfPayer(OPEN_PERIOD_OPENED_AT);
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
                givenGatheringOfPayer(OPEN_PERIOD_OPENED_AT);
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
                givenGatheringOfPayer(OPEN_PERIOD_OPENED_AT);
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
                givenGatheringOfPayer(OPEN_PERIOD_OPENED_AT);
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
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementRecordingClosedException() {
                // given
                givenGatheringOfPayer(CLOSED_PERIOD_OPENED_AT);
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
                givenGatheringOfPayer(OPEN_PERIOD_OPENED_AT);
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
                givenGatheringOfPayer(OPEN_PERIOD_OPENED_AT);
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
            @DisplayName("결제자를 뺀 참여자의 분담금을 미입금으로 계산해 저장하고 반환한다")
            void shouldSaveSettlementWithCalculatedShares() {
                // given
                givenRecordableGathering();
                givenSaveReturnsSettlement();
                givenScheduleLocked();
                RecordSettlementCommand command = command(9_000L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
                then(settlementRepository).should().save(captor.capture());
                assertThat(captor.getValue().getGatheringId()).isEqualTo(GATHERING_ID);
                assertThat(result.payerParticipantId()).isEqualTo(PAYER_ID);
                assertThat(result.totalAmount()).isEqualTo(9_000L);
                assertThat(result.notified()).isFalse();
                assertThat(result.shares())
                        .extracting(
                                SettlementShareResult::participantId,
                                SettlementShareResult::amount,
                                SettlementShareResult::fixed,
                                SettlementShareResult::status)
                        .containsExactly(
                                tuple(LEE_ID, 3_000L, false, ShareStatus.UNPAID),
                                tuple(PARK_ID, 3_000L, false, ShareStatus.UNPAID));
                assertThat(result.payerAmount()).isEqualTo(3_000L);
            }

            @Test
            @DisplayName("0원으로 지정한 분담금은 받을 돈이 없어 확인 완료로 저장한다")
            void shouldSaveZeroFixedShareAsConfirmed() {
                // given
                givenRecordableGathering();
                givenSaveReturnsSettlement();
                givenScheduleLocked();
                RecordSettlementCommand command =
                        command(10_000L, PARTICIPANT_IDS, Map.of(PARK_ID, 0L));

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.shares())
                        .extracting(
                                SettlementShareResult::participantId,
                                SettlementShareResult::amount,
                                SettlementShareResult::status)
                        .containsExactly(
                                tuple(LEE_ID, 5_000L, ShareStatus.UNPAID),
                                tuple(PARK_ID, 0L, ShareStatus.CONFIRMED));
                assertThat(result.confirmedAmount()).isZero();
            }
        }

        @Nested
        @DisplayName("남은 금액을 똑같이 나누는 경우")
        class WhenSplittingRemainingAmount {

            @Test
            @DisplayName("나누어떨어지면 결제자를 포함해 모두 같은 금액을 분담한다")
            void shouldSplitEquallyWhenDivisible() {
                // given
                givenRecordableGathering();
                givenSaveReturnsSettlement();
                givenScheduleLocked();
                RecordSettlementCommand command = command(10_050L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.shares())
                        .extracting(SettlementShareResult::amount)
                        .containsExactly(3_350L, 3_350L);
                assertThat(result.payerAmount()).isEqualTo(3_350L);
            }

            @Test
            @DisplayName("나누어떨어지지 않으면 몫을 1원 단위로 내리고 끝전을 결제자에게 귀속한다")
            void shouldRoundEqualShareDownAndAttributeRemainderToPayer() {
                // given
                givenRecordableGathering();
                givenSaveReturnsSettlement();
                givenScheduleLocked();
                RecordSettlementCommand command = command(10_001L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.shares())
                        .extracting(SettlementShareResult::amount)
                        .containsExactly(3_333L, 3_333L);
                assertThat(result.payerAmount()).isEqualTo(3_335L);
            }

            @Test
            @DisplayName("지정 금액은 그대로 두고, 지정하지 않은 참여자가 결제자와 남은 금액을 나눈다")
            void shouldKeepFixedAmountUnrounded() {
                // given
                givenRecordableGathering();
                givenSaveReturnsSettlement();
                givenScheduleLocked();
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
                                tuple(LEE_ID, 1_050L, true), tuple(PARK_ID, 4_475L, false));
                assertThat(result.payerAmount()).isEqualTo(4_475L);
            }

            @Test
            @DisplayName("다른 참여자가 모두 지정 금액이면 결제자가 남은 금액 전부를 분담한다")
            void shouldAttributeRemainingAmountToPayerWhenOthersFixed() {
                // given
                givenRecordableGathering();
                givenSaveReturnsSettlement();
                givenScheduleLocked();
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

        @Nested
        @DisplayName("발송 예약을 잡는 경우")
        class WhenScheduling {

            @Test
            @DisplayName("정산 건을 저장하기 전에 모임의 발송 예약을 잠그고, 기록 시각에 수정 시간 30분을 더한 시각으로 예약을 먼저 저장해 반환한다")
            void shouldLockAndSaveScheduleBeforeSavingSettlement() {
                // given
                givenRecordableGatheringAt(LocalDateTime.of(2026, 10, 28, 20, 0));
                RecordSettlementCommand command = command(9_000L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                ArgumentCaptor<NotificationSchedule> captor =
                        ArgumentCaptor.forClass(NotificationSchedule.class);
                InOrder inOrder = inOrder(notificationScheduleRepository, settlementRepository);
                inOrder.verify(notificationScheduleRepository).findOrCreateForUpdate(GATHERING_ID);
                inOrder.verify(notificationScheduleRepository).save(captor.capture());
                inOrder.verify(settlementRepository).save(any(Settlement.class));
                assertThat(captor.getValue().getScheduledAt())
                        .isEqualTo(LocalDateTime.of(2026, 10, 28, 20, 30));
                assertThat(result.notificationScheduledAt())
                        .isEqualTo(LocalDateTime.of(2026, 10, 28, 20, 30));
            }

            @Test
            @DisplayName("09:50에 기록하면 수정 시간을 다 받도록 같은 날 10:20으로 예약한다")
            void shouldScheduleAfterEditWindowWhenRecordedJustBeforeSendingStart() {
                // given
                givenRecordableGatheringAt(LocalDateTime.of(2026, 10, 28, 9, 50));
                RecordSettlementCommand command = command(9_000L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.notificationScheduledAt())
                        .isEqualTo(LocalDateTime.of(2026, 10, 28, 10, 20));
            }

            @Test
            @DisplayName("10:00에 기록하면 10:30으로 예약한다")
            void shouldScheduleAfterEditWindowWhenRecordedAtSendingStart() {
                // given
                givenRecordableGatheringAt(LocalDateTime.of(2026, 10, 28, 10, 0));
                RecordSettlementCommand command = command(9_000L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.notificationScheduledAt())
                        .isEqualTo(LocalDateTime.of(2026, 10, 28, 10, 30));
            }

            @Test
            @DisplayName("21:00에 기록하면 예약할 수 있는 마지막 시각인 21:30으로 예약한다")
            void shouldScheduleAtSchedulingEndWhenRecordedAtNinePm() {
                // given
                givenRecordableGatheringAt(LocalDateTime.of(2026, 10, 28, 21, 0));
                RecordSettlementCommand command = command(9_000L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.notificationScheduledAt())
                        .isEqualTo(LocalDateTime.of(2026, 10, 28, 21, 30));
            }

            @Test
            @DisplayName("21:01에 기록하면 다음 날 10:00으로 예약한다")
            void shouldScheduleNextDaySendingStartWhenRecordedAfterNinePm() {
                // given
                givenRecordableGatheringAt(LocalDateTime.of(2026, 10, 28, 21, 1));
                RecordSettlementCommand command = command(9_000L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.notificationScheduledAt())
                        .isEqualTo(LocalDateTime.of(2026, 10, 29, 10, 0));
            }

            @Test
            @DisplayName("00:30에 기록하면 같은 날 10:00으로 예약한다")
            void shouldScheduleSameDaySendingStartWhenRecordedAtDawn() {
                // given
                givenRecordableGatheringAt(LocalDateTime.of(2026, 10, 28, 0, 30));
                RecordSettlementCommand command = command(9_000L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.notificationScheduledAt())
                        .isEqualTo(LocalDateTime.of(2026, 10, 28, 10, 0));
            }
        }

        private RecordSettlementCommand command(
                long totalAmount, List<Long> participantIds, Map<Long, Long> fixedAmounts) {
            return new RecordSettlementCommand(
                    LINK_TOKEN, totalAmount, participantIds, fixedAmounts);
        }

        private void givenRecordableGathering() {
            givenGatheringOfPayer(OPEN_PERIOD_OPENED_AT);
            given(participantBankAccountRepository.existsByParticipantId(PAYER_ID))
                    .willReturn(true);
        }

        private void givenRecordableGatheringAt(LocalDateTime now) {
            givenNow(now);
            givenGatheringOfPayer(now.minusHours(1));
            given(participantBankAccountRepository.existsByParticipantId(PAYER_ID))
                    .willReturn(true);
            givenSaveReturnsSettlement();
            givenScheduleLocked();
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
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementRecordingClosedException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));
                givenGathering(CLOSED_PERIOD_OPENED_AT);
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
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenScheduleLocked();
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
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenScheduleLocked();
                UpdateSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(SettlementAlreadyCancelledException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("보냄이거나 0원보다 큰 확인 완료인 분담금이 있는 경우")
        class WhenSharesSent {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementSharesSentException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenScheduleLocked();
                UpdateSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(SettlementSharesSentException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("수정할 수 있는 경우")
        class WhenUpdatable {

            @Test
            @DisplayName("분담금을 다시 계산하고 기록 시각을 바꿔 저장한다")
            void shouldSaveRecalculatedSharesWithNewRecordedAt() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenSaveReturnsSettlement();
                givenScheduleLocked();
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

            @Test
            @DisplayName("확인 완료 분담금이 0원뿐이면 돈이 오가지 않았으므로 분담금을 다시 계산해 저장한다")
            void shouldSaveRecalculatedSharesWhenOnlyZeroShareConfirmed() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(settlementWithZeroShare());
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenSaveReturnsSettlement();
                givenScheduleLocked();
                UpdateSettlementCommand command = command();

                // when
                SettlementResult result = settlementCommandService.update(command);

                // then
                then(settlementRepository).should().save(any(Settlement.class));
                assertThat(result.totalAmount()).isEqualTo(20_000L);
                assertThat(result.shares())
                        .extracting(
                                SettlementShareResult::participantId,
                                SettlementShareResult::amount,
                                SettlementShareResult::status)
                        .containsExactly(tuple(LEE_ID, 10_000L, ShareStatus.UNPAID));
            }

            @Test
            @DisplayName("모임의 발송 예약을 잠그고, 바꾼 기록 시각으로 발송 시각을 다시 잡아 저장해 반환한다")
            void shouldRescheduleFromNewRecordedAt() {
                // given
                LocalDateTime now = LocalDateTime.of(2026, 10, 28, 20, 0);
                givenNow(now);
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));
                givenGathering(now.minusHours(1));
                givenSaveReturnsSettlement();
                givenScheduleLocked();
                UpdateSettlementCommand command = command();

                // when
                SettlementResult result = settlementCommandService.update(command);

                // then
                then(notificationScheduleRepository).should().findOrCreateForUpdate(GATHERING_ID);
                ArgumentCaptor<NotificationSchedule> captor =
                        ArgumentCaptor.forClass(NotificationSchedule.class);
                then(notificationScheduleRepository).should().save(captor.capture());
                assertThat(captor.getValue().getScheduledAt()).isEqualTo(now.plusMinutes(30));
                assertThat(result.recordedAt()).isEqualTo(now);
                assertThat(result.notificationScheduledAt()).isEqualTo(now.plusMinutes(30));
            }
        }

        private UpdateSettlementCommand command() {
            return new UpdateSettlementCommand(
                    LINK_TOKEN, SETTLEMENT_ID, 20_000L, List.of(PAYER_ID, LEE_ID), Map.of());
        }

        private void givenGathering(LocalDateTime openedAt) {
            given(gatheringRepository.findById(GATHERING_ID))
                    .willReturn(Optional.of(gathering(openedAt)));
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
        @DisplayName("보냄이거나 0원보다 큰 확인 완료인 분담금이 있는 경우")
        class WhenSharesSent {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementSharesSentException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));

                // when & then
                assertThatThrownBy(() -> settlementCommandService.cancel(command()))
                        .isInstanceOf(SettlementSharesSentException.class);
                thenSettlementNotSaved();
            }

            @Test
            @DisplayName("0원보다 큰 확인 완료 분담금이 있으면 예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementSharesSentExceptionWhenPositiveShareConfirmed() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.CONFIRMED));

                // when & then
                assertThatThrownBy(() -> settlementCommandService.cancel(command()))
                        .isInstanceOf(SettlementSharesSentException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("취소할 수 있는 경우")
        class WhenCancellable {

            @Test
            @DisplayName("기록 기간을 확인하지 않고 취소 시각을 채워 저장한다")
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
                then(gatheringRepository).shouldHaveNoInteractions();
                then(settlementRepository).should().save(settlement);
                assertThat(settlement.isCancelled()).isTrue();
            }

            @Test
            @DisplayName("확인 완료 분담금이 0원뿐이면 돈이 오가지 않았으므로 취소해 저장한다")
            void shouldSaveCancelledSettlementWhenOnlyZeroShareConfirmed() {
                // given
                Settlement settlement = settlementWithZeroShare();
                givenParticipant(PAYER_ID);
                givenSettlement(settlement);

                // when
                settlementCommandService.cancel(command());

                // then
                then(settlementRepository).should().save(settlement);
                assertThat(settlement.isCancelled()).isTrue();
            }

            @Test
            @DisplayName("정산 메시지를 보내기 전이면 취소 알림을 저장하지 않는다")
            void shouldNotSaveCancellationNoticesWhenSettlementNotNotified() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));

                // when
                settlementCommandService.cancel(command());

                // then
                then(notificationDeliveryRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("정산 메시지를 보낸 정산 건인 경우")
        class WhenSettlementNotified {

            // 정산 메시지가 닿지 않은 수신자다. 맡지 않은 PENDING, SKIPPED, FAILED 발송 기록의 수신자로 쓴다.
            private static final Long CHOI_ID = 23L;
            private static final Long JUNG_ID = 24L;
            private static final Long KANG_ID = 25L;

            @Test
            @DisplayName(
                    "보낸 정산 메시지와 컨슈머가 맡은 정산 메시지의 수신자에게만 취소 알림을 저장하고, 맡지 않았거나 건너뛰었거나 실패한 정산 메시지의 수신자는 뺀다")
            void shouldSaveCancellationNoticesOnlyForReachedRecipients() {
                // given
                givenNotifiedSettlementCancelledAt(LocalDateTime.of(2026, 10, 28, 10, 0));

                // when
                settlementCommandService.cancel(command());

                // then
                ArgumentCaptor<List<NotificationDelivery>> captor = ArgumentCaptor.captor();
                then(notificationDeliveryRepository).should().saveAll(captor.capture());
                assertThat(captor.getValue())
                        .extracting(NotificationDelivery::getParticipantId)
                        .containsExactly(LEE_ID, PARK_ID);
            }

            @Test
            @DisplayName("보낼 수 있는 시간대면 정산 메시지를 받은 참여자마다 바로 보낼 취소 알림을 저장한다")
            void shouldSaveCancellationNoticesAvailableNowWhenSendable() {
                // given
                LocalDateTime now = LocalDateTime.of(2026, 10, 28, 10, 0);
                givenNotifiedSettlementCancelledAt(now);

                // when
                settlementCommandService.cancel(command());

                // then
                thenCancellationNoticesSavedAvailableAt(now);
            }

            @Test
            @DisplayName("22:00 이후면 다음 날 10:00에 보낼 취소 알림을 저장한다")
            void shouldSaveCancellationNoticesAvailableNextDayWhenCancelledAtNight() {
                // given
                givenNotifiedSettlementCancelledAt(LocalDateTime.of(2026, 10, 28, 22, 0));

                // when
                settlementCommandService.cancel(command());

                // then
                thenCancellationNoticesSavedAvailableAt(LocalDateTime.of(2026, 10, 29, 10, 0));
            }

            @Test
            @DisplayName("10:00 전이면 같은 날 10:00에 보낼 취소 알림을 저장한다")
            void shouldSaveCancellationNoticesAvailableSameDayWhenCancelledBeforeSendingStart() {
                // given
                givenNotifiedSettlementCancelledAt(LocalDateTime.of(2026, 10, 28, 9, 59));

                // when
                settlementCommandService.cancel(command());

                // then
                thenCancellationNoticesSavedAvailableAt(LocalDateTime.of(2026, 10, 28, 10, 0));
            }

            @Test
            @DisplayName("그 정산 건을 담고 수신자에게 닿은 리마인드의 참여자에게도 취소 알림을 저장한다")
            void shouldSaveCancellationNoticesForReachedReminderRecipients() {
                // given
                LocalDateTime now = LocalDateTime.of(2026, 10, 28, 10, 0);
                givenNotifiedSettlementCancelledAt(now);
                givenReminders(
                        reminder(11L, CHOI_ID, List.of(SECOND_SETTLEMENT_ID, SETTLEMENT_ID))
                                .status(DeliveryStatus.SENT)
                                .claimedAt(now.minusHours(1))
                                .sentAt(now.minusHours(1))
                                .build(),
                        reminder(12L, JUNG_ID, List.of(SETTLEMENT_ID))
                                .claimedAt(now.minusMinutes(1))
                                .build());

                // when
                settlementCommandService.cancel(command());

                // then
                thenCancellationNoticesSavedFor(LEE_ID, PARK_ID, CHOI_ID, JUNG_ID);
            }

            @Test
            @DisplayName("다른 정산 건만 담은 리마인드와 컨슈머가 아직 맡지 않은 리마인드의 참여자는 뺀다")
            void shouldExcludeRecipientsOfRemindersNotReachedOrWithoutSettlement() {
                // given
                LocalDateTime now = LocalDateTime.of(2026, 10, 28, 10, 0);
                givenNotifiedSettlementCancelledAt(now);
                givenReminders(
                        reminder(11L, KANG_ID, List.of(SECOND_SETTLEMENT_ID))
                                .status(DeliveryStatus.SENT)
                                .claimedAt(now.minusHours(1))
                                .sentAt(now.minusHours(1))
                                .build(),
                        reminder(12L, CHOI_ID, List.of(SETTLEMENT_ID)).build());

                // when
                settlementCommandService.cancel(command());

                // then
                thenCancellationNoticesSavedFor(LEE_ID, PARK_ID);
            }

            @Test
            @DisplayName("정산 메시지와 리마인드를 모두 받은 참여자에게는 취소 알림을 하나만 저장한다")
            void shouldSaveOneCancellationNoticeForRecipientOfNoticeAndReminder() {
                // given
                LocalDateTime now = LocalDateTime.of(2026, 10, 28, 10, 0);
                givenNotifiedSettlementCancelledAt(now);
                givenReminders(
                        reminder(11L, LEE_ID, List.of(SETTLEMENT_ID))
                                .status(DeliveryStatus.SENT)
                                .claimedAt(now.minusMinutes(30))
                                .sentAt(now.minusMinutes(30))
                                .build());

                // when
                settlementCommandService.cancel(command());

                // then
                thenCancellationNoticesSavedFor(LEE_ID, PARK_ID);
            }

            // 이는 정산 메시지를 받았고, 박은 컨슈머가 맡아 내용을 만들었다. 나머지 수신자에게는 정산 메시지가 닿지 않았다.
            private void givenNotifiedSettlementCancelledAt(LocalDateTime now) {
                givenNow(now);
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createNotified(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID, PARK_ID, CHOI_ID, JUNG_ID, KANG_ID)));
                given(
                                notificationDeliveryRepository.findAllSettlementDeliveriesForUpdate(
                                        SETTLEMENT_ID))
                        .willReturn(
                                List.of(
                                        settlementNotice(1L, LEE_ID)
                                                .status(DeliveryStatus.SENT)
                                                .claimedAt(now.minusHours(1))
                                                .sentAt(now.minusHours(1))
                                                .build(),
                                        settlementNotice(2L, CHOI_ID).build(),
                                        settlementNotice(3L, PARK_ID)
                                                .claimedAt(now.minusMinutes(1))
                                                .build(),
                                        settlementNotice(4L, JUNG_ID)
                                                .status(DeliveryStatus.SKIPPED)
                                                .build(),
                                        settlementNotice(5L, KANG_ID)
                                                .status(DeliveryStatus.FAILED)
                                                .attempts(5)
                                                .build()));
            }

            private NotificationDelivery.NotificationDeliveryBuilder settlementNotice(
                    Long deliveryId, Long participantId) {
                return NotificationDeliveryFixture.SETTLEMENT_NOTICE.builder(
                        deliveryId, GATHERING_ID, participantId, List.of(SETTLEMENT_ID));
            }

            private NotificationDelivery.NotificationDeliveryBuilder reminder(
                    Long deliveryId, Long participantId, List<Long> settlementIds) {
                return NotificationDeliveryFixture.REMINDER_NOTICE.builder(
                        deliveryId, GATHERING_ID, participantId, settlementIds);
            }

            private void givenReminders(NotificationDelivery... reminders) {
                given(
                                notificationDeliveryRepository
                                        .findAllRemindersByGatheringIdForUpdate(GATHERING_ID))
                        .willReturn(List.of(reminders));
            }

            private void thenCancellationNoticesSavedFor(Long... participantIds) {
                ArgumentCaptor<List<NotificationDelivery>> captor = ArgumentCaptor.captor();
                then(notificationDeliveryRepository).should().saveAll(captor.capture());
                assertThat(captor.getValue())
                        .extracting(NotificationDelivery::getParticipantId)
                        .containsExactly(participantIds);
            }

            private void thenCancellationNoticesSavedAvailableAt(LocalDateTime availableAt) {
                ArgumentCaptor<List<NotificationDelivery>> captor = ArgumentCaptor.captor();
                then(notificationDeliveryRepository).should().saveAll(captor.capture());
                assertThat(captor.getValue())
                        .extracting(
                                NotificationDelivery::getGatheringId,
                                NotificationDelivery::getParticipantId,
                                NotificationDelivery::getType,
                                NotificationDelivery::getSettlementIds,
                                NotificationDelivery::getStatus,
                                NotificationDelivery::getAvailableAt)
                        .containsExactly(
                                tuple(
                                        GATHERING_ID,
                                        LEE_ID,
                                        NotificationType.CANCELLATION,
                                        List.of(SETTLEMENT_ID),
                                        DeliveryStatus.PENDING,
                                        availableAt),
                                tuple(
                                        GATHERING_ID,
                                        PARK_ID,
                                        NotificationType.CANCELLATION,
                                        List.of(SETTLEMENT_ID),
                                        DeliveryStatus.PENDING,
                                        availableAt));
            }
        }

        private CancelSettlementCommand command() {
            return CancelSettlementCommand.of(LINK_TOKEN, SETTLEMENT_ID);
        }
    }

    @Nested
    @DisplayName("markSent 메서드는")
    class MarkSent {

        @Nested
        @DisplayName("개인 링크 토큰에 해당하는 참여자가 없는 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                given(participantLinkTokenRepository.findParticipantIdByValue(LINK_TOKEN))
                        .willReturn(Optional.empty());
                MarkSharesSentCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.markSent(command))
                        .isInstanceOf(LinkTokenNotFoundException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("그 결제자에게 낼 분담금이 없는 경우")
        class WhenSharesNotFound {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementShareNotFoundException() {
                // given
                givenParticipant(LEE_ID);
                givenSharedSettlements();
                MarkSharesSentCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.markSent(command))
                        .isInstanceOf(SettlementShareNotFoundException.class);
                thenSettlementNotSaved();
                then(gatheringRepository).shouldHaveNoInteractions();
                then(participantBankAccountRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("그 결제자에게 낼 분담금이 모두 미입금인 경우")
        class WhenSharesUnpaid {

            @Test
            @DisplayName("요청한 참여자와 결제자로 정산 건을 잠가 읽고, 정산 건마다 분담금을 보냄으로 바꿔 저장해 결제자 계좌와 함께 반환한다")
            void shouldSaveSentShareForEachSettlementOfPayer() {
                // given
                givenParticipant(LEE_ID);
                givenSharedSettlements(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS),
                        SettlementFixture.SECOND_ROUND.createWithId(
                                THIRD_SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID)));
                givenGatheringWithPayerBankAccount();
                givenSaveReturnsSettlement();

                // when
                PayerSharesResult result = settlementCommandService.markSent(command());

                // then
                then(settlementRepository)
                        .should()
                        .findAllByShareParticipantIdAndPayerForUpdate(LEE_ID, PAYER_ID);
                ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
                then(settlementRepository).should(times(2)).save(captor.capture());
                assertThat(captor.getAllValues())
                        .extracting(Settlement::getId)
                        .containsExactly(SETTLEMENT_ID, THIRD_SETTLEMENT_ID);
                assertThat(result.payerParticipantId()).isEqualTo(PAYER_ID);
                assertThat(result.payerName()).isEqualTo(ParticipantFixture.KIM.getName());
                assertThat(result.bankName())
                        .isEqualTo(ParticipantBankAccountFixture.KOOKMIN.getBankName());
                assertThat(result.accountNumber())
                        .isEqualTo(ParticipantBankAccountFixture.KOOKMIN.getAccountNumber());
                assertThat(result.unpaidAmount()).isZero();
                assertThat(result.hasUnpaidShares()).isFalse();
                assertThat(result.shares())
                        .extracting(
                                ShareBreakdownResult::settlementId,
                                ShareBreakdownResult::amount,
                                ShareBreakdownResult::status)
                        .containsExactly(
                                tuple(SETTLEMENT_ID, 3_333L, ShareStatus.SENT),
                                tuple(THIRD_SETTLEMENT_ID, 10_000L, ShareStatus.SENT));
            }
        }

        @Nested
        @DisplayName("이미 보냄이나 확인 완료인 분담금이 있는 경우")
        class WhenSharesAlreadySent {

            @Test
            @DisplayName("그 정산 건은 저장하지 않고 미입금인 정산 건만 보냄으로 바꿔 저장한다")
            void shouldSaveOnlyUnpaidSettlement() {
                // given
                givenParticipant(LEE_ID);
                givenSharedSettlements(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT),
                        SettlementFixture.SECOND_ROUND.createWithFirstShareStatus(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.CONFIRMED),
                        SettlementFixture.DINNER.createWithId(
                                THIRD_SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));
                givenGatheringWithPayerBankAccount();
                givenSaveReturnsSettlement();

                // when
                PayerSharesResult result = settlementCommandService.markSent(command());

                // then
                ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
                then(settlementRepository).should().save(captor.capture());
                assertThat(captor.getValue().getId()).isEqualTo(THIRD_SETTLEMENT_ID);
                assertThat(result.shares())
                        .extracting(
                                ShareBreakdownResult::settlementId, ShareBreakdownResult::status)
                        .containsExactly(
                                tuple(SETTLEMENT_ID, ShareStatus.SENT),
                                tuple(SECOND_SETTLEMENT_ID, ShareStatus.CONFIRMED),
                                tuple(THIRD_SETTLEMENT_ID, ShareStatus.SENT));
            }

            @Test
            @DisplayName("미입금 분담금이 없으면 아무것도 저장하지 않고 예외 없이 반환한다")
            void shouldReturnWithoutSavingWhenUnpaidSharesNotExist() {
                // given
                givenParticipant(LEE_ID);
                givenSharedSettlements(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT),
                        SettlementFixture.SECOND_ROUND.createWithFirstShareStatus(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.CONFIRMED));
                givenGatheringWithPayerBankAccount();

                // when
                PayerSharesResult result = settlementCommandService.markSent(command());

                // then
                thenSettlementNotSaved();
                assertThat(result.hasUnpaidShares()).isFalse();
                assertThat(result.unpaidAmount()).isZero();
                assertThat(result.shares())
                        .extracting(ShareBreakdownResult::status)
                        .containsExactly(ShareStatus.SENT, ShareStatus.CONFIRMED);
            }
        }

        private MarkSharesSentCommand command() {
            return new MarkSharesSentCommand(LINK_TOKEN, PAYER_ID);
        }

        private void givenSharedSettlements(Settlement... settlements) {
            given(
                            settlementRepository.findAllByShareParticipantIdAndPayerForUpdate(
                                    LEE_ID, PAYER_ID))
                    .willReturn(List.of(settlements));
        }

        private void givenGatheringWithPayerBankAccount() {
            given(gatheringRepository.findByParticipantId(LEE_ID))
                    .willReturn(Optional.of(gathering(OPEN_PERIOD_OPENED_AT)));
            given(
                            participantBankAccountRepository.findAllByParticipantIds(
                                    List.of(PAYER_ID), GATHERING_ID))
                    .willReturn(List.of(ParticipantBankAccountFixture.KOOKMIN.create(PAYER_ID)));
        }
    }

    @Nested
    @DisplayName("confirm 메서드는")
    class Confirm {

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
                ConfirmShareCommand command = command(LEE_ID);

                // when & then
                assertThatThrownBy(() -> settlementCommandService.confirm(command))
                        .isInstanceOf(NotSettlementPayerException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("이미 확인 완료인 분담금인 경우")
        class WhenShareConfirmed {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowShareAlreadyConfirmedException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.CONFIRMED));
                ConfirmShareCommand command = command(LEE_ID);

                // when & then
                assertThatThrownBy(() -> settlementCommandService.confirm(command))
                        .isInstanceOf(ShareAlreadyConfirmedException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("확인 완료로 시작한 0원 분담금인 경우")
        class WhenShareAmountZero {

            @Test
            @DisplayName("이미 확인 완료가 아니라 0원 분담금이라는 예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowZeroShareStatusUnchangeableException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(settlementWithZeroShare());
                ConfirmShareCommand command = command(PARK_ID);

                // when & then
                assertThatThrownBy(() -> settlementCommandService.confirm(command))
                        .isInstanceOf(ZeroShareStatusUnchangeableException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("정산 건에 분담금이 없는 참여자인 경우")
        class WhenShareNotFound {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementShareNotFoundException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));
                ConfirmShareCommand command = command(OUTSIDER_ID);

                // when & then
                assertThatThrownBy(() -> settlementCommandService.confirm(command))
                        .isInstanceOf(SettlementShareNotFoundException.class);
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
                ConfirmShareCommand command = command(LEE_ID);

                // when & then
                assertThatThrownBy(() -> settlementCommandService.confirm(command))
                        .isInstanceOf(SettlementAlreadyCancelledException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("확인 완료로 바꿀 수 있는 경우")
        class WhenConfirmable {

            @Test
            @DisplayName("보냄 분담금을 확인 완료로 바꿔 저장하고 확인한 금액을 담아 반환한다")
            void shouldConfirmSentShare() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));
                givenSaveReturnsSettlement();

                // when
                SettlementResult result = settlementCommandService.confirm(command(LEE_ID));

                // then
                then(settlementRepository).should().save(any(Settlement.class));
                assertThat(result.shares())
                        .extracting(
                                SettlementShareResult::participantId, SettlementShareResult::status)
                        .containsExactly(
                                tuple(LEE_ID, ShareStatus.CONFIRMED),
                                tuple(PARK_ID, ShareStatus.UNPAID));
                assertThat(result.confirmedAmount()).isEqualTo(3_333L);
                assertThat(result.sentAmount()).isZero();
            }

            @Test
            @DisplayName("미입금 분담금도 확인 완료로 바꾸고, 보냄 분담금은 따로 더해 반환한다")
            void shouldConfirmUnpaidShare() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));
                givenSaveReturnsSettlement();

                // when
                SettlementResult result = settlementCommandService.confirm(command(PARK_ID));

                // then
                assertThat(result.shares())
                        .extracting(
                                SettlementShareResult::participantId, SettlementShareResult::status)
                        .containsExactly(
                                tuple(LEE_ID, ShareStatus.SENT),
                                tuple(PARK_ID, ShareStatus.CONFIRMED));
                assertThat(result.confirmedAmount()).isEqualTo(3_333L);
                assertThat(result.sentAmount()).isEqualTo(3_333L);
            }

            @Test
            @DisplayName("정산 메시지를 보내기 전이면 모임의 발송 예정 시각을 담아 반환한다")
            void shouldReturnNotificationScheduledAtWhenSettlementNotNotified() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));
                givenSaveReturnsSettlement();
                givenSchedule();

                // when
                SettlementResult result = settlementCommandService.confirm(command(LEE_ID));

                // then
                assertThat(result.notificationScheduledAt()).isEqualTo(SCHEDULED_AT);
            }

            @Test
            @DisplayName("정산 메시지를 보낸 정산 건이면 모임에 다른 예약이 있어도 발송 예정 시각을 비워 반환한다")
            void shouldReturnNullNotificationScheduledAtWhenSettlementNotified() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createNotified(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));
                givenSaveReturnsSettlement();
                givenSchedule();

                // when
                SettlementResult result = settlementCommandService.confirm(command(LEE_ID));

                // then
                assertThat(result.notified()).isTrue();
                assertThat(result.notificationScheduledAt()).isNull();
            }
        }

        private ConfirmShareCommand command(Long participantId) {
            return ConfirmShareCommand.of(LINK_TOKEN, SETTLEMENT_ID, participantId);
        }
    }

    @Nested
    @DisplayName("revert 메서드는")
    class Revert {

        @Nested
        @DisplayName("요청한 참여자가 결제자가 아닌 경우")
        class WhenParticipantIsNotPayer {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowNotSettlementPayerException() {
                // given
                givenParticipant(LEE_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));
                RevertShareCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.revert(command))
                        .isInstanceOf(NotSettlementPayerException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("이미 미입금인 분담금인 경우")
        class WhenShareUnpaid {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowShareAlreadyUnpaidException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));
                RevertShareCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.revert(command))
                        .isInstanceOf(ShareAlreadyUnpaidException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("확인 완료로 시작한 0원 분담금인 경우")
        class WhenShareAmountZero {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowZeroShareStatusUnchangeableException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(settlementWithZeroShare());
                RevertShareCommand command =
                        RevertShareCommand.of(LINK_TOKEN, SETTLEMENT_ID, PARK_ID);

                // when & then
                assertThatThrownBy(() -> settlementCommandService.revert(command))
                        .isInstanceOf(ZeroShareStatusUnchangeableException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("미입금으로 되돌릴 수 있는 경우")
        class WhenRevertible {

            @Test
            @DisplayName("보냄 분담금을 미입금으로 되돌려 저장하고 반환한다")
            void shouldRevertSentShare() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));
                givenSaveReturnsSettlement();

                // when
                SettlementResult result = settlementCommandService.revert(command());

                // then
                then(settlementRepository).should().save(any(Settlement.class));
                assertThat(result.shares())
                        .extracting(SettlementShareResult::status)
                        .containsOnly(ShareStatus.UNPAID);
                assertThat(result.sentAmount()).isZero();
            }

            @Test
            @DisplayName("확인 완료 분담금도 미입금으로 되돌려 저장하고 반환한다")
            void shouldRevertConfirmedShare() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.CONFIRMED));
                givenSaveReturnsSettlement();

                // when
                SettlementResult result = settlementCommandService.revert(command());

                // then
                then(settlementRepository).should().save(any(Settlement.class));
                assertThat(result.shares())
                        .extracting(SettlementShareResult::status)
                        .containsOnly(ShareStatus.UNPAID);
                assertThat(result.confirmedAmount()).isZero();
            }
        }

        @Nested
        @DisplayName("정산 메시지를 보내기 전의 정산 건인 경우")
        class WhenSettlementNotNotified {

            @Test
            @DisplayName("모임의 발송 예정 시각을 담아 반환한다")
            void shouldReturnNotificationScheduledAt() {
                // given
                givenUnnotifiedSettlement();
                givenSchedule();

                // when
                SettlementResult result = settlementCommandService.revert(command());

                // then
                assertThat(result.notificationScheduledAt()).isEqualTo(SCHEDULED_AT);
            }

            @Test
            @DisplayName("발송 예약을 잠그지 않고 리마인드를 저장하지 않는다")
            void shouldNotScheduleReminder() {
                // given
                givenUnnotifiedSettlement();

                // when
                settlementCommandService.revert(command());

                // then
                then(notificationScheduleRepository)
                        .should(never())
                        .findOrCreateForUpdate(anyLong());
                then(notificationDeliveryRepository).shouldHaveNoInteractions();
            }

            private void givenUnnotifiedSettlement() {
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));
                givenSaveReturnsSettlement();
            }
        }

        @Nested
        @DisplayName("정산 메시지를 보낸 정산 건의 분담금을 되돌리는 경우")
        class WhenSettlementNotified {

            @Test
            @DisplayName("모임에 다른 예약이 있어도 발송 예정 시각을 비워 반환한다")
            void shouldReturnNullNotificationScheduledAt() {
                // given
                givenNotifiedSettlementRevertedAt(NOW);
                givenSchedule();

                // when
                SettlementResult result = settlementCommandService.revert(command());

                // then
                assertThat(result.notified()).isTrue();
                assertThat(result.notificationScheduledAt()).isNull();
            }

            @Test
            @DisplayName("정산 건을 저장하기 전에 모임의 발송 예약을 잠그고, 되돌린 참여자에게 24시간 뒤 그 정산 건을 담은 리마인드를 저장한다")
            void shouldLockScheduleAndSaveReminderBeforeSavingSettlement() {
                // given
                givenNotifiedSettlementRevertedAt(NOW);

                // when
                settlementCommandService.revert(command());

                // then
                ArgumentCaptor<List<NotificationDelivery>> captor = ArgumentCaptor.captor();
                InOrder inOrder =
                        inOrder(
                                notificationScheduleRepository,
                                notificationDeliveryRepository,
                                settlementRepository);
                inOrder.verify(notificationScheduleRepository).findOrCreateForUpdate(GATHERING_ID);
                inOrder.verify(notificationDeliveryRepository).saveAll(captor.capture());
                inOrder.verify(settlementRepository).save(any(Settlement.class));
                assertThat(captor.getValue())
                        .extracting(
                                NotificationDelivery::getGatheringId,
                                NotificationDelivery::getParticipantId,
                                NotificationDelivery::getType,
                                NotificationDelivery::getSettlementIds,
                                NotificationDelivery::getStatus,
                                NotificationDelivery::getAvailableAt)
                        .containsExactly(
                                tuple(
                                        GATHERING_ID,
                                        LEE_ID,
                                        NotificationType.REMINDER,
                                        List.of(SETTLEMENT_ID),
                                        DeliveryStatus.PENDING,
                                        LocalDateTime.of(2026, 10, 30, 18, 0)));
            }

            @Test
            @DisplayName("21:59:59에 되돌리면 다음 날 21:59:59에 보낼 리마인드를 저장한다")
            void shouldSaveReminderNextDayWhenRevertedJustBeforeSendingEnd() {
                // given
                givenNotifiedSettlementRevertedAt(LocalDateTime.of(2026, 10, 29, 21, 59, 59));

                // when
                settlementCommandService.revert(command());

                // then
                thenReminderSavedAt(LocalDateTime.of(2026, 10, 30, 21, 59, 59));
            }

            @Test
            @DisplayName("22:00에 되돌리면 24시간 뒤가 보낼 수 없는 시간대라 그다음 10:00에 보낼 리마인드를 저장한다")
            void shouldSaveReminderAtSendingStartWhenRevertedAtSendingEnd() {
                // given
                givenNotifiedSettlementRevertedAt(LocalDateTime.of(2026, 10, 29, 22, 0));

                // when
                settlementCommandService.revert(command());

                // then
                thenReminderSavedAt(LocalDateTime.of(2026, 10, 31, 10, 0));
            }

            @Test
            @DisplayName("되돌린 참여자의 보내기를 기다리는 리마인드가 새 리마인드 시각과 24시간에서 1초 모자란 차이면 리마인드를 저장하지 않는다")
            void shouldNotSaveReminderWhenPendingReminderWithinGap() {
                // given
                givenNotifiedSettlementRevertedAt(NOW);
                givenPendingReminderOfLeeAt(LocalDateTime.of(2026, 10, 31, 17, 59, 59));

                // when
                settlementCommandService.revert(command());

                // then
                then(notificationDeliveryRepository).should().saveAll(List.of());
                then(settlementRepository).should().save(any(Settlement.class));
            }

            @Test
            @DisplayName("되돌린 참여자의 보내기를 기다리는 리마인드가 새 리마인드 시각과 정확히 24시간 차이면 리마인드를 저장한다")
            void shouldSaveReminderWhenPendingReminderAtGap() {
                // given
                givenNotifiedSettlementRevertedAt(NOW);
                givenPendingReminderOfLeeAt(LocalDateTime.of(2026, 10, 31, 18, 0));

                // when
                settlementCommandService.revert(command());

                // then
                thenReminderSavedAt(LocalDateTime.of(2026, 10, 30, 18, 0));
            }

            // 이의 분담금을 보냄으로 바꾼 뒤 정산 메시지를 보낸 정산 건이다.
            private void givenNotifiedSettlementRevertedAt(LocalDateTime now) {
                givenNow(now);
                Settlement settlement =
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT);
                settlement.markNotified(SettlementFixture.DINNER.getRecordedAt().plusHours(1));
                givenParticipant(PAYER_ID);
                givenSettlement(settlement);
                givenSaveReturnsSettlement();
            }

            private void givenPendingReminderOfLeeAt(LocalDateTime availableAt) {
                given(notificationDeliveryRepository.findAllRemindersByGatheringId(GATHERING_ID))
                        .willReturn(
                                List.of(
                                        NotificationDeliveryFixture.REMINDER_NOTICE
                                                .builder(
                                                        1L,
                                                        GATHERING_ID,
                                                        LEE_ID,
                                                        List.of(SECOND_SETTLEMENT_ID))
                                                .availableAt(availableAt)
                                                .build()));
            }

            private void thenReminderSavedAt(LocalDateTime availableAt) {
                ArgumentCaptor<List<NotificationDelivery>> captor = ArgumentCaptor.captor();
                then(notificationDeliveryRepository).should().saveAll(captor.capture());
                assertThat(captor.getValue())
                        .extracting(NotificationDelivery::getAvailableAt)
                        .containsExactly(availableAt);
            }
        }

        private RevertShareCommand command() {
            return RevertShareCommand.of(LINK_TOKEN, SETTLEMENT_ID, LEE_ID);
        }
    }

    private Gathering gathering(LocalDateTime openedAt) {
        return GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                GATHERING_ID,
                openedAt,
                List.of(
                        ParticipantFixture.KIM.createWithId(PAYER_ID),
                        ParticipantFixture.LEE.createWithId(LEE_ID),
                        ParticipantFixture.PARK.createWithId(PARK_ID)));
    }

    private void givenParticipant(Long participantId) {
        given(participantLinkTokenRepository.findParticipantIdByValue(LINK_TOKEN))
                .willReturn(Optional.of(participantId));
    }

    private void givenGatheringOfPayer(LocalDateTime openedAt) {
        givenParticipant(PAYER_ID);
        given(gatheringRepository.findByParticipantId(PAYER_ID))
                .willReturn(Optional.of(gathering(openedAt)));
    }

    private void givenSettlement(Settlement settlement) {
        given(settlementRepository.findById(SETTLEMENT_ID)).willReturn(Optional.of(settlement));
    }

    private void givenSaveReturnsSettlement() {
        given(settlementRepository.save(any(Settlement.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenScheduleLocked() {
        given(notificationScheduleRepository.findOrCreateForUpdate(GATHERING_ID))
                .willReturn(NotificationSchedule.empty(GATHERING_ID));
        given(notificationScheduleRepository.save(any(NotificationSchedule.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenSchedule() {
        given(notificationScheduleRepository.findByGatheringId(GATHERING_ID))
                .willReturn(
                        Optional.of(
                                NotificationSchedule.builder()
                                        .gatheringId(GATHERING_ID)
                                        .scheduledAt(SCHEDULED_AT)
                                        .build()));
    }

    private void givenNow(LocalDateTime now) {
        settlementCommandService =
                settlementCommandService(
                        Clock.fixed(
                                now.atZone(TimeConfig.SERVICE_ZONE).toInstant(),
                                TimeConfig.SERVICE_ZONE));
    }

    private SettlementCommandService settlementCommandService(Clock clock) {
        NotificationCommandService notificationCommandService =
                new NotificationCommandService(
                        notificationScheduleRepository, notificationDeliveryRepository, clock);
        return new SettlementCommandService(
                participantLinkTokenRepository,
                gatheringRepository,
                participantBankAccountRepository,
                settlementRepository,
                notificationCommandService,
                notificationCommandService,
                new NotificationQueryService(notificationScheduleRepository, clock),
                notificationCommandService,
                clock);
    }

    private void thenSettlementNotSaved() {
        then(settlementRepository).should(never()).save(any(Settlement.class));
    }

    // 박의 분담금을 0원으로 지정해 확인 완료로 시작하고, 이의 분담금은 미입금인 정산 건이다.
    private Settlement settlementWithZeroShare() {
        return SettlementFixture.DINNER.createWithFixedAmounts(
                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS, Map.of(PARK_ID, 0L));
    }
}
