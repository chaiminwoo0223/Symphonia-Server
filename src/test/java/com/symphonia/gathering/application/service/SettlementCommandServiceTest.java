package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.command.CancelSettlementCommand;
import com.symphonia.gathering.application.dto.command.RecordSettlementCommand;
import com.symphonia.gathering.application.dto.command.UpdateSettlementCommand;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.dto.result.SettlementShareResult;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.FixedAmountExceedsTotalException;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.GatheringRoundNotFoundException;
import com.symphonia.gathering.domain.exception.InvalidSettlementAmountException;
import com.symphonia.gathering.domain.exception.InvalidSettlementParticipantsException;
import com.symphonia.gathering.domain.exception.NotGatheringHostException;
import com.symphonia.gathering.domain.exception.ParticipantNotFoundException;
import com.symphonia.gathering.domain.exception.SettlementAlreadyCancelledException;
import com.symphonia.gathering.domain.exception.SettlementNotFoundException;
import com.symphonia.gathering.domain.exception.SettlementRecordingClosedException;
import com.symphonia.gathering.domain.exception.SettlementSharesSentException;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.ShareStatus;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringRetentionFixture;
import com.symphonia.gathering.fixture.GatheringRoundFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.global.config.TimeConfig;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@DisplayName("SettlementCommandService 단위 테스트")
class SettlementCommandServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long OTHER_GATHERING_ID = 11L;
    private static final Long ROUND_ID = GatheringRoundFixture.FIRST.getId();
    private static final Long SECOND_ROUND_ID = GatheringRoundFixture.SECOND.getId();
    private static final Long OTHER_GATHERING_ROUND_ID = 999L;
    private static final Long SETTLEMENT_ID = 30L;
    private static final Long PAYER_ID = 20L;
    private static final Long LEE_ID = 21L;
    private static final Long PARK_ID = 22L;
    private static final Long LATE_JOINER_ID = 23L;
    private static final Long OUTSIDER_ID = 99L;
    private static final Long HOST_MEMBER_ID = GatheringFixture.FRIDAY_DINNER.getHostMemberId();
    private static final Long OTHER_MEMBER_ID = 99L;
    private static final List<Long> PARTICIPANT_IDS = List.of(PAYER_ID, LEE_ID, PARK_ID);
    private static final UUID CLIENT_REQUEST_ID =
            UUID.fromString("0b6c1a52-6f4e-4d0a-9d52-7a1f3c1e8b11");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 18, 0);

    // 기록 기간은 모임을 만든 지 48시간이 되는 순간 닫힌다.
    // 닫히기 1초 전과 닫히는 순간을 경계로 쓴다.
    private static final LocalDateTime OPEN_PERIOD_OPENED_AT = NOW.minusHours(48).plusSeconds(1);
    private static final LocalDateTime CLOSED_PERIOD_OPENED_AT = NOW.minusHours(48);

    private SettlementCommandService settlementCommandService;

    @Mock private GatheringRepository gatheringRepository;
    @Mock private GatheringRetentionRepository gatheringRetentionRepository;
    @Mock private SettlementRepository settlementRepository;

    @BeforeEach
    void setUp() {
        givenNow(NOW);
    }

    @Nested
    @DisplayName("record 메서드는")
    class Record {

        @Nested
        @DisplayName("모임의 파기 시각이 없는 경우")
        class WhenRetentionNotFound {

            @Test
            @DisplayName("예외를 던지고 모임을 읽지 않으며 정산 건을 저장하지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                        .willReturn(Optional.empty());
                RecordSettlementCommand command = command(10_000L, PARTICIPANT_IDS, Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 지난 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName("물리 삭제 전이라도 모임을 찾지 못한 예외를 던지고 모임을 읽지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                givenRetention(GatheringRetentionFixture.EXPIRED);
                RecordSettlementCommand command = command(10_000L, PARTICIPANT_IDS, Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임이 없는 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                givenRetention(GatheringRetentionFixture.RETAINED);
                given(gatheringRepository.findById(GATHERING_ID)).willReturn(Optional.empty());
                RecordSettlementCommand command = command(10_000L, PARTICIPANT_IDS, Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원이 아닌 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowNotGatheringHostException() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                RecordSettlementCommand command =
                        command(
                                OTHER_MEMBER_ID,
                                PAYER_ID,
                                ROUND_ID,
                                10_000L,
                                PARTICIPANT_IDS,
                                Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(NotGatheringHostException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("결제자가 이 모임의 참여자가 아닌 경우")
        class WhenPayerNotInGathering {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowParticipantNotFoundException() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                RecordSettlementCommand command =
                        command(
                                HOST_MEMBER_ID,
                                OUTSIDER_ID,
                                ROUND_ID,
                                10_000L,
                                List.of(OUTSIDER_ID, LEE_ID),
                                Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(ParticipantNotFoundException.class);
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
                givenGathering(CLOSED_PERIOD_OPENED_AT);
                RecordSettlementCommand command = command(10_000L, PARTICIPANT_IDS, Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(SettlementRecordingClosedException.class);
                thenSettlementNotRecorded();
            }
        }

        @Nested
        @DisplayName("이 모임의 차수가 아닌 차수 id인 경우")
        class WhenRoundNotInGathering {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowGatheringRoundNotFoundException() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                RecordSettlementCommand command =
                        command(OTHER_GATHERING_ROUND_ID, 10_000L, PARTICIPANT_IDS, Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(GatheringRoundNotFoundException.class);
                thenSettlementNotRecorded();
            }
        }

        @Nested
        @DisplayName("인원에 이 모임의 참여자가 아닌 id가 있는 경우")
        class WhenParticipantNotInGathering {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowInvalidSettlementParticipantsException() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                RecordSettlementCommand command =
                        command(10_000L, List.of(PAYER_ID, OUTSIDER_ID), Map.of());

                // when & then
                assertThatThrownBy(() -> settlementCommandService.record(command))
                        .isInstanceOf(InvalidSettlementParticipantsException.class);
                thenSettlementNotRecorded();
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
                thenSettlementNotRecorded();
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
                thenSettlementNotRecorded();
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
                thenSettlementNotRecorded();
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
                thenSettlementNotRecorded();
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
                thenSettlementNotRecorded();
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
                thenSettlementNotRecorded();
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
                thenSettlementNotRecorded();
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
                thenSettlementNotRecorded();
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
                thenSettlementNotRecorded();
            }
        }

        @Nested
        @DisplayName("기록할 수 있는 경우")
        class WhenRecordable {

            @Test
            @DisplayName("모임의 파기 시각 1초 전이면 정산 건을 저장한다")
            void shouldSaveSettlementWhenGatheringAboutToExpire() {
                // given
                givenRetention(GatheringRetentionFixture.ABOUT_TO_EXPIRE);
                given(gatheringRepository.findById(GATHERING_ID))
                        .willReturn(Optional.of(gathering(OPEN_PERIOD_OPENED_AT)));
                givenSaveIfAbsentReturnsSettlement();
                RecordSettlementCommand command = command(9_000L, PARTICIPANT_IDS, Map.of());

                // when
                settlementCommandService.record(command);

                // then
                then(settlementRepository).should().saveIfAbsent(any(Settlement.class));
            }

            @Test
            @DisplayName("요청 id를 담아 정산 건을 저장한다")
            void shouldSaveSettlementWithClientRequestId() {
                // given
                givenRecordableGathering();
                givenSaveIfAbsentReturnsSettlement();
                RecordSettlementCommand command = command(9_000L, PARTICIPANT_IDS, Map.of());

                // when
                settlementCommandService.record(command);

                // then
                ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
                then(settlementRepository).should().saveIfAbsent(captor.capture());
                assertThat(captor.getValue().getClientRequestId()).isEqualTo(CLIENT_REQUEST_ID);
            }

            @Test
            @DisplayName("결제자를 뺀 참여자의 분담금을 미입금으로 계산해 저장하고 반환한다")
            void shouldSaveSettlementWithCalculatedShares() {
                // given
                givenRecordableGathering();
                givenSaveIfAbsentReturnsSettlement();
                RecordSettlementCommand command = command(9_000L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
                then(settlementRepository).should().saveIfAbsent(captor.capture());
                assertThat(captor.getValue().getGatheringId()).isEqualTo(GATHERING_ID);
                assertThat(result.payerParticipantId()).isEqualTo(PAYER_ID);
                assertThat(result.totalAmount()).isEqualTo(9_000L);
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
                assertThat(result.receivableAmount()).isEqualTo(6_000L);
            }

            @Test
            @DisplayName("고른 차수를 정산 건에 담아 저장하고, 그 차수의 id와 번호를 반환한다")
            void shouldSaveSettlementOfRound() {
                // given
                givenRecordableGathering();
                givenSaveIfAbsentReturnsSettlement();
                RecordSettlementCommand command =
                        command(SECOND_ROUND_ID, 9_000L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
                then(settlementRepository).should().saveIfAbsent(captor.capture());
                assertThat(captor.getValue().getRoundId()).isEqualTo(SECOND_ROUND_ID);
                assertThat(result.roundId()).isEqualTo(SECOND_ROUND_ID);
                assertThat(result.roundNumber())
                        .isEqualTo(GatheringRoundFixture.SECOND.getNumber());
            }

            @Test
            @DisplayName("0원으로 지정한 분담금은 받을 돈이 없어 확인 완료로 저장한다")
            void shouldSaveZeroFixedShareAsConfirmed() {
                // given
                givenRecordableGathering();
                givenSaveIfAbsentReturnsSettlement();
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
                givenSaveIfAbsentReturnsSettlement();
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
                givenSaveIfAbsentReturnsSettlement();
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
                givenSaveIfAbsentReturnsSettlement();
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
                givenSaveIfAbsentReturnsSettlement();
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
        @DisplayName("같은 차수에 결제자가 다른 정산 건을 카드 분할로 기록하는 경우")
        class WhenSplitPaymentInSameRound {

            @Test
            @DisplayName("첫 결제자의 정산 건은 고른 차수의 id와 번호를 반환한다")
            void shouldReturnRoundWhenFirstPayerRecords() {
                // given
                givenRecordableGathering();
                givenSaveIfAbsentReturnsSettlement();
                RecordSettlementCommand command =
                        command(SECOND_ROUND_ID, 6_000L, List.of(PAYER_ID, LEE_ID), Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.payerParticipantId()).isEqualTo(PAYER_ID);
                assertThat(result.roundId()).isEqualTo(SECOND_ROUND_ID);
                assertThat(result.roundNumber())
                        .isEqualTo(GatheringRoundFixture.SECOND.getNumber());
            }

            @Test
            @DisplayName("다른 결제자의 정산 건도 같은 차수의 id와 번호를 반환한다")
            void shouldReturnSameRoundWhenOtherPayerRecords() {
                // given
                givenRecordableGathering();
                givenSaveIfAbsentReturnsSettlement();
                RecordSettlementCommand command =
                        command(
                                HOST_MEMBER_ID,
                                LEE_ID,
                                SECOND_ROUND_ID,
                                4_000L,
                                List.of(LEE_ID, PARK_ID),
                                Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.payerParticipantId()).isEqualTo(LEE_ID);
                assertThat(result.roundId()).isEqualTo(SECOND_ROUND_ID);
                assertThat(result.roundNumber())
                        .isEqualTo(GatheringRoundFixture.SECOND.getNumber());
            }
        }

        @Nested
        @DisplayName("이전 차수보다 적은 인원으로 기록하는 경우")
        class WhenParticipantsFewerThanPreviousRound {

            @Test
            @DisplayName("고른 차수의 번호와 그 인원 기준으로 만든 분담금을 반환한다")
            void shouldCalculateSharesOfSelectedParticipants() {
                // given
                List<Long> gatheringParticipantIds = participantIds(10);
                List<Long> roundParticipantIds =
                        List.of(
                                gatheringParticipantIds.get(0),
                                gatheringParticipantIds.get(3),
                                gatheringParticipantIds.get(5),
                                gatheringParticipantIds.get(8));
                givenRecordableGathering(gatheringParticipantIds);
                givenSaveIfAbsentReturnsSettlement();
                RecordSettlementCommand command =
                        command(SECOND_ROUND_ID, 10_000L, roundParticipantIds, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.roundNumber())
                        .isEqualTo(GatheringRoundFixture.SECOND.getNumber());
                assertThat(result.shares())
                        .extracting(
                                SettlementShareResult::participantId, SettlementShareResult::amount)
                        .containsExactly(
                                tuple(roundParticipantIds.get(1), 2_500L),
                                tuple(roundParticipantIds.get(2), 2_500L),
                                tuple(roundParticipantIds.get(3), 2_500L));
                assertThat(result.payerAmount()).isEqualTo(2_500L);
            }
        }

        @Nested
        @DisplayName("이전 차수를 기록한 뒤에 모임에 합류한 참여자를 인원에 넣는 경우")
        class WhenParticipantJoinedAfterPreviousRound {

            @Test
            @DisplayName("예외 없이 합류한 참여자의 분담금을 포함해 저장한다")
            void shouldSaveSettlementWithLateJoiner() {
                // given
                givenRecordableGathering(List.of(PAYER_ID, LEE_ID, PARK_ID, LATE_JOINER_ID));
                givenSaveIfAbsentReturnsSettlement();
                RecordSettlementCommand command =
                        command(
                                SECOND_ROUND_ID,
                                9_000L,
                                List.of(PAYER_ID, LEE_ID, LATE_JOINER_ID),
                                Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                then(settlementRepository).should().saveIfAbsent(any(Settlement.class));
                assertThat(result.shares())
                        .extracting(
                                SettlementShareResult::participantId, SettlementShareResult::amount)
                        .containsExactly(tuple(LEE_ID, 3_000L), tuple(LATE_JOINER_ID, 3_000L));
            }
        }

        @Nested
        @DisplayName("같은 요청 id로 이미 기록한 정산 건이 있는 경우")
        class WhenSameClientRequestIdRecorded {

            @Test
            @DisplayName("요청한 차수가 이 모임의 차수가 아니어도 처음 기록한 정산 건과 그 차수를 반환하고 저장하지 않는다")
            void shouldReturnRecordedSettlementWithItsRoundWithoutValidatingRequest() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                given(
                                settlementRepository.findByPayerParticipantIdAndClientRequestId(
                                        PAYER_ID, CLIENT_REQUEST_ID))
                        .willReturn(Optional.of(recordedSecondRoundSettlement()));
                RecordSettlementCommand command =
                        command(OTHER_GATHERING_ROUND_ID, 9_000L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.id()).isEqualTo(SETTLEMENT_ID);
                assertThat(result.totalAmount())
                        .isEqualTo(SettlementFixture.SECOND_ROUND.getTotalAmount());
                assertThat(result.roundId()).isEqualTo(SECOND_ROUND_ID);
                assertThat(result.roundNumber())
                        .isEqualTo(GatheringRoundFixture.SECOND.getNumber());
                then(settlementRepository).should(never()).saveIfAbsent(any(Settlement.class));
            }
        }

        @Nested
        @DisplayName("저장하는 사이 같은 요청 id가 먼저 기록된 경우")
        class WhenSameClientRequestIdRecordedConcurrently {

            @Test
            @DisplayName("먼저 기록된 정산 건과 그 차수를 반환한다")
            void shouldReturnRecordedSettlementWithItsRound() {
                // given
                givenRecordableGathering();
                given(settlementRepository.saveIfAbsent(any(Settlement.class)))
                        .willReturn(recordedSecondRoundSettlement());
                RecordSettlementCommand command = command(9_000L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.id()).isEqualTo(SETTLEMENT_ID);
                assertThat(result.roundId()).isEqualTo(SECOND_ROUND_ID);
                assertThat(result.roundNumber())
                        .isEqualTo(GatheringRoundFixture.SECOND.getNumber());
            }

            @Test
            @DisplayName("먼저 읽은 모임에 없는 차수에 기록됐으면 모임을 다시 읽어 그 차수를 반환한다")
            void shouldReadGatheringAgainWhenRoundMissingInSnapshot() {
                // given
                givenRetention(GatheringRetentionFixture.RETAINED);
                given(gatheringRepository.findById(GATHERING_ID))
                        .willReturn(
                                Optional.of(
                                        GatheringFixture.FRIDAY_DINNER.createWithRounds(
                                                GATHERING_ID,
                                                OPEN_PERIOD_OPENED_AT,
                                                ParticipantFixture.createWithIds(PARTICIPANT_IDS),
                                                List.of(GatheringRoundFixture.FIRST.create()))),
                                Optional.of(gathering(OPEN_PERIOD_OPENED_AT)));
                given(settlementRepository.saveIfAbsent(any(Settlement.class)))
                        .willReturn(recordedSecondRoundSettlement());
                RecordSettlementCommand command = command(9_000L, PARTICIPANT_IDS, Map.of());

                // when
                SettlementResult result = settlementCommandService.record(command);

                // then
                assertThat(result.roundId()).isEqualTo(SECOND_ROUND_ID);
                assertThat(result.roundNumber())
                        .isEqualTo(GatheringRoundFixture.SECOND.getNumber());
            }
        }

        // 요청 id로 기록된 정산 건을 먼저 찾으므로, 저장소를 아예 부르지 않았는지가 아니라 저장하지 않았는지를 확인한다.
        private void thenSettlementNotRecorded() {
            then(settlementRepository).should(never()).saveIfAbsent(any(Settlement.class));
        }

        private Settlement recordedSecondRoundSettlement() {
            return SettlementFixture.SECOND_ROUND.createWithId(
                    SETTLEMENT_ID,
                    GATHERING_ID,
                    SECOND_ROUND_ID,
                    PAYER_ID,
                    List.of(PAYER_ID, LEE_ID));
        }

        private RecordSettlementCommand command(
                long totalAmount, List<Long> participantIds, Map<Long, Long> fixedAmounts) {
            return command(ROUND_ID, totalAmount, participantIds, fixedAmounts);
        }

        private RecordSettlementCommand command(
                Long roundId,
                long totalAmount,
                List<Long> participantIds,
                Map<Long, Long> fixedAmounts) {
            return command(
                    HOST_MEMBER_ID, PAYER_ID, roundId, totalAmount, participantIds, fixedAmounts);
        }

        private RecordSettlementCommand command(
                Long memberId,
                Long payerParticipantId,
                Long roundId,
                long totalAmount,
                List<Long> participantIds,
                Map<Long, Long> fixedAmounts) {
            return new RecordSettlementCommand(
                    memberId,
                    GATHERING_ID,
                    payerParticipantId,
                    CLIENT_REQUEST_ID,
                    roundId,
                    totalAmount,
                    participantIds,
                    fixedAmounts);
        }

        private void givenRecordableGathering() {
            givenRecordableGathering(PARTICIPANT_IDS);
        }

        // 모임의 참여자가 gatheringParticipantIds인 기록 가능한 모임이다.
        private void givenRecordableGathering(List<Long> gatheringParticipantIds) {
            givenGathering(
                    OPEN_PERIOD_OPENED_AT,
                    ParticipantFixture.createWithIds(gatheringParticipantIds));
        }

        // 결제자 id(20)부터 1씩 늘어나는 count명의 참여자 id다.
        private List<Long> participantIds(int count) {
            return LongStream.range(PAYER_ID, PAYER_ID + count).boxed().toList();
        }
    }

    @Nested
    @DisplayName("update 메서드는")
    class Update {

        @Nested
        @DisplayName("모임의 파기 시각이 없는 경우")
        class WhenRetentionNotFound {

            @Test
            @DisplayName("예외를 던지고 모임과 정산 건을 읽지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                        .willReturn(Optional.empty());
                UpdateSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 지난 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName("물리 삭제 전이라도 모임을 찾지 못한 예외를 던지고 모임과 정산 건을 읽지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                givenRetention(GatheringRetentionFixture.EXPIRED);
                UpdateSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임이 없는 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("예외를 던지고 정산 건을 읽지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                givenRetention(GatheringRetentionFixture.RETAINED);
                given(gatheringRepository.findById(GATHERING_ID)).willReturn(Optional.empty());
                UpdateSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원이 아닌 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("예외를 던지고 정산 건을 읽거나 저장하지 않는다")
            void shouldThrowNotGatheringHostException() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                UpdateSettlementCommand command = command(OTHER_MEMBER_ID, ROUND_ID);

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(NotGatheringHostException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("정산 건이 없는 경우")
        class WhenSettlementNotFound {

            @Test
            @DisplayName("예외를 던진다")
            void shouldThrowSettlementNotFoundException() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                given(settlementRepository.findById(SETTLEMENT_ID)).willReturn(Optional.empty());
                UpdateSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(SettlementNotFoundException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("다른 모임의 정산 건인 경우")
        class WhenSettlementOfOtherGathering {

            @Test
            @DisplayName("정산 건이 없는 경우와 같게 예외를 던지고 저장하지 않는다")
            void shouldThrowSettlementNotFoundException() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID,
                                OTHER_GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS));
                UpdateSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(SettlementNotFoundException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementRecordingClosedException() {
                // given
                givenGathering(CLOSED_PERIOD_OPENED_AT);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS));
                UpdateSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(SettlementRecordingClosedException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("이 모임의 차수가 아닌 차수 id인 경우")
        class WhenRoundNotInGathering {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowGatheringRoundNotFoundException() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS));
                UpdateSettlementCommand command = command(OTHER_GATHERING_ROUND_ID);

                // when & then
                assertThatThrownBy(() -> settlementCommandService.update(command))
                        .isInstanceOf(GatheringRoundNotFoundException.class);
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
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenSettlement(
                        SettlementFixture.DINNER.createCancelled(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS));
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
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));
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
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS));
                givenSaveReturnsSettlement();
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
            @DisplayName("정산 건의 결제자를 그대로 두고 그 결제자 기준으로 분담금을 계산한다")
            void shouldKeepPayerOfSettlement() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, LEE_ID, PARTICIPANT_IDS));
                givenSaveReturnsSettlement();
                UpdateSettlementCommand command = command();

                // when
                SettlementResult result = settlementCommandService.update(command);

                // then
                assertThat(result.payerParticipantId()).isEqualTo(LEE_ID);
                assertThat(result.shares())
                        .extracting(
                                SettlementShareResult::participantId, SettlementShareResult::amount)
                        .containsExactly(tuple(PAYER_ID, 10_000L));
            }

            @Test
            @DisplayName("차수를 바꿔 저장하고, 새 차수의 id와 번호를 반환한다")
            void shouldSaveSettlementWithNewRound() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS));
                givenSaveReturnsSettlement();
                UpdateSettlementCommand command = command(SECOND_ROUND_ID);

                // when
                SettlementResult result = settlementCommandService.update(command);

                // then
                ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
                then(settlementRepository).should().save(captor.capture());
                assertThat(captor.getValue().getRoundId()).isEqualTo(SECOND_ROUND_ID);
                assertThat(result.roundId()).isEqualTo(SECOND_ROUND_ID);
                assertThat(result.roundNumber())
                        .isEqualTo(GatheringRoundFixture.SECOND.getNumber());
            }

            @Test
            @DisplayName("확인 완료 분담금이 0원뿐이면 돈이 오가지 않았으므로 분담금을 다시 계산해 저장한다")
            void shouldSaveRecalculatedSharesWhenOnlyZeroShareConfirmed() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenSettlement(settlementWithZeroShare());
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

            @Test
            @DisplayName("기록 시각을 수정한 시각으로 바꿔 저장해 반환한다")
            void shouldUpdateRecordedAt() {
                // given
                LocalDateTime now = LocalDateTime.of(2026, 10, 28, 20, 0);
                givenNow(now);
                givenGathering(now.minusHours(1));
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS));
                givenSaveReturnsSettlement();
                UpdateSettlementCommand command = command();

                // when
                SettlementResult result = settlementCommandService.update(command);

                // then
                assertThat(result.recordedAt()).isEqualTo(now);
            }
        }

        private UpdateSettlementCommand command() {
            return command(ROUND_ID);
        }

        private UpdateSettlementCommand command(Long roundId) {
            return command(HOST_MEMBER_ID, roundId);
        }

        private UpdateSettlementCommand command(Long memberId, Long roundId) {
            return new UpdateSettlementCommand(
                    memberId,
                    GATHERING_ID,
                    SETTLEMENT_ID,
                    roundId,
                    20_000L,
                    List.of(PAYER_ID, LEE_ID),
                    Map.of());
        }
    }

    @Nested
    @DisplayName("cancel 메서드는")
    class Cancel {

        @Nested
        @DisplayName("모임의 파기 시각이 없는 경우")
        class WhenRetentionNotFound {

            @Test
            @DisplayName("예외를 던지고 모임과 정산 건을 읽지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                        .willReturn(Optional.empty());
                CancelSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.cancel(command))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 지난 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName("물리 삭제 전이라도 모임을 찾지 못한 예외를 던지고 모임과 정산 건을 읽지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                givenRetention(GatheringRetentionFixture.EXPIRED);
                CancelSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.cancel(command))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원이 아닌 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("예외를 던지고 정산 건을 읽거나 저장하지 않는다")
            void shouldThrowNotGatheringHostException() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                CancelSettlementCommand command =
                        CancelSettlementCommand.of(OTHER_MEMBER_ID, GATHERING_ID, SETTLEMENT_ID);

                // when & then
                assertThatThrownBy(() -> settlementCommandService.cancel(command))
                        .isInstanceOf(NotGatheringHostException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("정산 건이 없는 경우")
        class WhenSettlementNotFound {

            @Test
            @DisplayName("예외를 던진다")
            void shouldThrowSettlementNotFoundException() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                given(settlementRepository.findById(SETTLEMENT_ID)).willReturn(Optional.empty());
                CancelSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.cancel(command))
                        .isInstanceOf(SettlementNotFoundException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("다른 모임의 정산 건인 경우")
        class WhenSettlementOfOtherGathering {

            @Test
            @DisplayName("정산 건이 없는 경우와 같게 예외를 던지고 저장하지 않는다")
            void shouldThrowSettlementNotFoundException() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                Settlement settlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID,
                                OTHER_GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS);
                givenSettlement(settlement);
                CancelSettlementCommand command = command();

                // when & then
                assertThatThrownBy(() -> settlementCommandService.cancel(command))
                        .isInstanceOf(SettlementNotFoundException.class);
                thenSettlementNotSaved();
                assertThat(settlement.isCancelled()).isFalse();
            }
        }

        @Nested
        @DisplayName("이미 취소한 정산 건인 경우")
        class WhenSettlementCancelled {

            @Test
            @DisplayName("같은 요청을 다시 보낸 것으로 보고 취소 시각을 바꾸거나 정산 건을 저장하지 않고 끝낸다")
            void shouldKeepCancelledAtWithoutSaving() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT);
                Settlement settlement =
                        SettlementFixture.DINNER.createCancelled(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS);
                LocalDateTime cancelledAt = settlement.getCancelledAt();
                givenSettlement(settlement);

                // when
                settlementCommandService.cancel(command());

                // then
                assertThat(settlement.getCancelledAt()).isEqualTo(cancelledAt);
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
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
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
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
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
            @DisplayName("기록 기간이 끝난 모임이어도 취소 시각을 채워 저장한다")
            void shouldSaveCancelledSettlementEvenWhenRecordingPeriodClosed() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS);
                givenGathering(CLOSED_PERIOD_OPENED_AT);
                givenSettlement(settlement);

                // when
                settlementCommandService.cancel(command());

                // then
                then(settlementRepository).should().save(settlement);
                assertThat(settlement.isCancelled()).isTrue();
            }

            @Test
            @DisplayName("확인 완료 분담금이 0원뿐이면 돈이 오가지 않았으므로 취소해 저장한다")
            void shouldSaveCancelledSettlementWhenOnlyZeroShareConfirmed() {
                // given
                Settlement settlement = settlementWithZeroShare();
                givenGathering(OPEN_PERIOD_OPENED_AT);
                givenSettlement(settlement);

                // when
                settlementCommandService.cancel(command());

                // then
                then(settlementRepository).should().save(settlement);
                assertThat(settlement.isCancelled()).isTrue();
            }
        }

        private CancelSettlementCommand command() {
            return CancelSettlementCommand.of(HOST_MEMBER_ID, GATHERING_ID, SETTLEMENT_ID);
        }
    }

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

    private void givenRetention(GatheringRetentionFixture retention) {
        given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                .willReturn(Optional.of(retention.create(GATHERING_ID, NOW)));
    }

    private void givenGathering(LocalDateTime openedAt) {
        givenGathering(openedAt, ParticipantFixture.createWithIds(PARTICIPANT_IDS));
    }

    private void givenGathering(LocalDateTime openedAt, List<Participant> participants) {
        givenRetention(GatheringRetentionFixture.RETAINED);
        given(gatheringRepository.findById(GATHERING_ID))
                .willReturn(Optional.of(gathering(openedAt, participants)));
    }

    private void givenSettlement(Settlement settlement) {
        given(settlementRepository.findById(SETTLEMENT_ID)).willReturn(Optional.of(settlement));
    }

    private void givenSaveIfAbsentReturnsSettlement() {
        given(settlementRepository.saveIfAbsent(any(Settlement.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenSaveReturnsSettlement() {
        given(settlementRepository.save(any(Settlement.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenNow(LocalDateTime now) {
        Clock clock =
                Clock.fixed(
                        now.atZone(TimeConfig.SERVICE_ZONE).toInstant(), TimeConfig.SERVICE_ZONE);
        settlementCommandService =
                new SettlementCommandService(
                        gatheringRepository,
                        gatheringRetentionRepository,
                        settlementRepository,
                        clock);
    }

    private void thenSettlementNotSaved() {
        then(settlementRepository).should(never()).save(any(Settlement.class));
    }

    // 박의 분담금을 0원으로 지정해 확인 완료로 시작하고, 이의 분담금은 미입금인 정산 건이다.
    private Settlement settlementWithZeroShare() {
        return SettlementFixture.DINNER.createWithFixedAmounts(
                SETTLEMENT_ID,
                GATHERING_ID,
                ROUND_ID,
                PAYER_ID,
                PARTICIPANT_IDS,
                Map.of(PARK_ID, 0L));
    }
}
