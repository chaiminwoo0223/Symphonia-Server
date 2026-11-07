package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.command.CancelSettlementCommand;
import com.symphonia.gathering.application.dto.command.RecordSettlementCommand;
import com.symphonia.gathering.application.dto.command.UpdateSettlementCommand;
import com.symphonia.gathering.application.dto.result.LinkedParticipantResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.dto.result.SettlementShareResult;
import com.symphonia.gathering.application.usecase.GetLinkedParticipantUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.BankAccountRequiredException;
import com.symphonia.gathering.domain.exception.FixedAmountExceedsTotalException;
import com.symphonia.gathering.domain.exception.InvalidSettlementAmountException;
import com.symphonia.gathering.domain.exception.InvalidSettlementParticipantsException;
import com.symphonia.gathering.domain.exception.LinkTokenNotFoundException;
import com.symphonia.gathering.domain.exception.NotSettlementPayerException;
import com.symphonia.gathering.domain.exception.SettlementAlreadyCancelledException;
import com.symphonia.gathering.domain.exception.SettlementAlreadyNotifiedException;
import com.symphonia.gathering.domain.exception.SettlementNotFoundException;
import com.symphonia.gathering.domain.exception.SettlementRecordingClosedException;
import com.symphonia.gathering.domain.exception.SettlementSharesSentException;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.ShareStatus;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.global.config.TimeConfig;
import com.symphonia.notification.application.service.NotificationCommandService;
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
    private static final Long PAYER_ID = 20L;
    private static final Long LEE_ID = 21L;
    private static final Long PARK_ID = 22L;
    private static final Long OUTSIDER_ID = 99L;
    private static final List<Long> PARTICIPANT_IDS = List.of(PAYER_ID, LEE_ID, PARK_ID);
    private static final String LINK_TOKEN = "link-token";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 18, 0);
    private static final LocalDateTime DELETES_AT = NOW.plusDays(7);

    // 기록 기간은 모임을 만든 지 48시간이 되는 순간 닫힌다.
    // 닫히기 1초 전과 닫히는 순간을 경계로 쓴다.
    private static final LocalDateTime OPEN_PERIOD_OPENED_AT = NOW.minusHours(48).plusSeconds(1);
    private static final LocalDateTime CLOSED_PERIOD_OPENED_AT = NOW.minusHours(48);

    private SettlementCommandService settlementCommandService;

    @Mock private GetLinkedParticipantUseCase getLinkedParticipantUseCase;
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
    @DisplayName("record 메서드는")
    class Record {

        // 토큰이 없거나 모임의 파기 시각이 지났거나 데이터 키를 지운 경우다. 갈래별 판단은 ParticipantQueryServiceTest가 검증한다.
        @Nested
        @DisplayName("개인 링크 토큰으로 참여자를 찾지 못한 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 모임을 읽지 않으며 정산 건을 저장하지 않는다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                givenLinkTokenNotFound();
                RecordSettlementCommand command = command(10_000L, PARTICIPANT_IDS, Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(LinkTokenNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

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

        // 토큰이 없거나 모임의 파기 시각이 지났거나 데이터 키를 지운 경우다. 갈래별 판단은 ParticipantQueryServiceTest가 검증한다.
        @Nested
        @DisplayName("개인 링크 토큰으로 참여자를 찾지 못한 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 정산 건과 모임을 읽지 않는다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                givenLinkTokenNotFound();
                UpdateSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(LinkTokenNotFoundException.class);
                then(settlementRepository).shouldHaveNoInteractions();
                then(gatheringRepository).shouldHaveNoInteractions();
            }
        }

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

        // 토큰이 없거나 모임의 파기 시각이 지났거나 데이터 키를 지운 경우다. 갈래별 판단은 ParticipantQueryServiceTest가 검증한다.
        @Nested
        @DisplayName("개인 링크 토큰으로 참여자를 찾지 못한 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 정산 건을 읽지 않는다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                givenLinkTokenNotFound();
                CancelSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.cancel(command))
                        .isInstanceOf(LinkTokenNotFoundException.class);
                then(settlementRepository).shouldHaveNoInteractions();
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
        given(getLinkedParticipantUseCase.getLinkedParticipant(LINK_TOKEN))
                .willReturn(new LinkedParticipantResult(participantId, DELETES_AT));
    }

    private void givenLinkTokenNotFound() {
        given(getLinkedParticipantUseCase.getLinkedParticipant(LINK_TOKEN))
                .willThrow(new LinkTokenNotFoundException());
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

    private void givenNow(LocalDateTime now) {
        Clock clock =
                Clock.fixed(
                        now.atZone(TimeConfig.SERVICE_ZONE).toInstant(), TimeConfig.SERVICE_ZONE);
        NotificationCommandService notificationCommandService =
                new NotificationCommandService(
                        notificationScheduleRepository, notificationDeliveryRepository, clock);
        settlementCommandService =
                new SettlementCommandService(
                        getLinkedParticipantUseCase,
                        gatheringRepository,
                        participantBankAccountRepository,
                        settlementRepository,
                        notificationCommandService,
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
