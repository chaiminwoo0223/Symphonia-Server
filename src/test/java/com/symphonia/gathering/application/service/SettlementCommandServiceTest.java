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
    private static final Long SECOND_SETTLEMENT_ID = 31L;
    private static final Long THIRD_SETTLEMENT_ID = 32L;
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
            @DisplayName("0원으로 지정한 분담은 받을 돈이 없어 확인 완료로 저장한다")
            void shouldSaveZeroFixedShareAsConfirmed() {
                // given
                givenRecordableGathering();
                givenSaveReturnsSettlement();
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
        @DisplayName("보냄이거나 0원보다 큰 확인 완료인 분담이 있는 경우")
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
                givenGathering(LocalDateTime.now().minusHours(1));
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

            @Test
            @DisplayName("확인 완료 분담이 0원뿐이면 돈이 오가지 않았으므로 분담을 다시 계산해 저장한다")
            void shouldSaveRecalculatedSharesWhenOnlyZeroShareConfirmed() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(settlementWithZeroShare());
                givenGathering(LocalDateTime.now().minusHours(1));
                givenSaveReturnsSettlement();
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
        @DisplayName("보냄이거나 0원보다 큰 확인 완료인 분담이 있는 경우")
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
            @DisplayName("0원보다 큰 확인 완료 분담이 있으면 예외를 던지고 정산 건을 저장하지 않는다")
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
            @DisplayName("확인 완료 분담이 0원뿐이면 돈이 오가지 않았으므로 취소해 저장한다")
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
        @DisplayName("그 결제자에게 낼 분담이 없는 경우")
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
        @DisplayName("그 결제자에게 낼 분담이 모두 미입금인 경우")
        class WhenSharesUnpaid {

            @Test
            @DisplayName("요청한 참여자와 결제자로 정산 건을 잠가 읽고, 정산 건마다 분담을 보냄으로 바꿔 저장해 결제자 계좌와 함께 반환한다")
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
        @DisplayName("이미 보냄이나 확인 완료인 분담이 있는 경우")
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
            @DisplayName("미입금 분담이 없으면 아무것도 저장하지 않고 예외 없이 반환한다")
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
                    .willReturn(Optional.of(gathering(LocalDateTime.now().minusHours(1))));
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
        @DisplayName("이미 확인 완료인 분담인 경우")
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
        @DisplayName("확인 완료로 시작한 0원 분담인 경우")
        class WhenShareAmountZero {

            @Test
            @DisplayName("이미 확인 완료가 아니라 0원 분담이라는 예외를 던지고 정산 건을 저장하지 않는다")
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
        @DisplayName("정산 건에 분담이 없는 참여자인 경우")
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
            @DisplayName("보냄 분담을 확인 완료로 바꿔 저장하고 확인한 금액을 담아 반환한다")
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
            @DisplayName("미입금 분담도 확인 완료로 바꾸고, 보냄 분담의 금액은 따로 담아 반환한다")
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
        @DisplayName("이미 미입금인 분담인 경우")
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
        @DisplayName("확인 완료로 시작한 0원 분담인 경우")
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
            @DisplayName("보냄 분담을 미입금으로 되돌려 저장하고 반환한다")
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
            @DisplayName("확인 완료 분담도 미입금으로 되돌려 저장하고 반환한다")
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

        private RevertShareCommand command() {
            return RevertShareCommand.of(LINK_TOKEN, SETTLEMENT_ID, LEE_ID);
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

    private void givenSaveReturnsSettlement() {
        given(settlementRepository.save(any(Settlement.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    private void thenSettlementNotSaved() {
        then(settlementRepository).should(never()).save(any(Settlement.class));
    }

    // 박의 분담을 0원으로 지정해 확인 완료로 시작하고, 이의 분담은 미입금인 정산 건이다.
    private Settlement settlementWithZeroShare() {
        return SettlementFixture.DINNER.createWithFixedAmounts(
                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS, Map.of(PARK_ID, 0L));
    }
}
