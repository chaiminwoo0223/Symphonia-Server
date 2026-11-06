package com.symphonia.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.provider.NotificationTargetProviderImpl;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.ShareStatus;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.global.config.TimeConfig;
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
import org.mockito.Mock;

@DisplayName("DispatchSettlementNotificationService 단위 테스트")
class DispatchSettlementNotificationServiceTest extends UnitTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 15, 0);
    private static final LocalDateTime THREE_DAYS_LATER = LocalDateTime.of(2026, 10, 31, 15, 0);
    private static final LocalDateTime FIVE_DAYS_LATER = LocalDateTime.of(2026, 11, 2, 15, 0);
    private static final Long GATHERING_ID = 10L;
    private static final Long SETTLEMENT_ID = 30L;
    private static final Long SECOND_SETTLEMENT_ID = 31L;
    private static final Long THIRD_SETTLEMENT_ID = 32L;
    private static final Long PAYER_ID = 20L;
    private static final Long LEE_ID = 21L;
    private static final Long PARK_ID = 22L;
    private static final List<Long> PARTICIPANT_IDS = List.of(PAYER_ID, LEE_ID, PARK_ID);

    private DispatchSettlementNotificationService dispatchSettlementNotificationService;

    @Mock private NotificationScheduleRepository notificationScheduleRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private NotificationDeliveryRepository notificationDeliveryRepository;

    @BeforeEach
    void setUp() {
        givenNow(NOW);
    }

    @Nested
    @DisplayName("dispatch 메서드는")
    class Dispatch {

        @Nested
        @DisplayName("예약 시각이 지난 예약이 없는 경우")
        class WhenScheduleNotDue {

            @Test
            @DisplayName("정산 건과 발송 기록, 예약을 건드리지 않는다")
            void shouldDoNothing() {
                // given
                given(notificationScheduleRepository.findDueForUpdate(GATHERING_ID, NOW))
                        .willReturn(Optional.empty());

                // when
                dispatchSettlementNotificationService.dispatch(GATHERING_ID);

                // then
                then(settlementRepository).shouldHaveNoInteractions();
                then(notificationDeliveryRepository).shouldHaveNoInteractions();
                then(notificationScheduleRepository)
                        .should(never())
                        .save(any(NotificationSchedule.class));
            }
        }

        @Nested
        @DisplayName("보낼 수 있는 시간대인 경우")
        class WhenSendable {

            @Test
            @DisplayName("보내지 않은 정산 건마다 발송 시각을 남겨 저장하고, 예약 시각을 비워 저장한다")
            void shouldMarkSettlementsNotifiedAndClearSchedule() {
                // given
                NotificationSchedule schedule = givenDueSchedule(NOW);
                List<Settlement> settlements = givenUnnotifiedSettlements();

                // when
                dispatchSettlementNotificationService.dispatch(GATHERING_ID);

                // then
                then(settlementRepository).should(times(3)).save(any(Settlement.class));
                assertThat(settlements).extracting(Settlement::getNotifiedAt).containsOnly(NOW);
                then(notificationScheduleRepository).should().save(schedule);
                assertThat(schedule.isScheduled()).isFalse();
            }

            @Test
            @DisplayName("0원이거나 보냄, 확인 완료인 분담금은 빼고 참여자마다 미입금 분담금이 있는 정산 건을 묶어 발송 기록을 저장한다")
            void shouldSaveSettlementNoticesGroupedByParticipant() {
                // given
                givenDueSchedule(NOW);
                givenUnnotifiedSettlements();

                // when
                dispatchSettlementNotificationService.dispatch(GATHERING_ID);

                // then
                ArgumentCaptor<List<NotificationDelivery>> captor = ArgumentCaptor.captor();
                then(notificationDeliveryRepository).should().saveAll(captor.capture());
                assertThat(captor.getValue())
                        .filteredOn(delivery -> delivery.getType() == NotificationType.SETTLEMENT)
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
                                        PARK_ID,
                                        NotificationType.SETTLEMENT,
                                        List.of(SETTLEMENT_ID, SECOND_SETTLEMENT_ID),
                                        DeliveryStatus.PENDING,
                                        NOW),
                                tuple(
                                        GATHERING_ID,
                                        PAYER_ID,
                                        NotificationType.SETTLEMENT,
                                        List.of(THIRD_SETTLEMENT_ID),
                                        DeliveryStatus.PENDING,
                                        NOW));
            }

            @Test
            @DisplayName("수신자마다 정산 메시지를 보내기 시작한 시각부터 3일 뒤와 5일 뒤에 수신자의 정산 건을 담은 리마인드를 저장한다")
            void shouldSaveRemindersAfterThreeAndFiveDaysForEachRecipient() {
                // given
                givenDueSchedule(NOW);
                givenUnnotifiedSettlements();

                // when
                dispatchSettlementNotificationService.dispatch(GATHERING_ID);

                // then
                ArgumentCaptor<List<NotificationDelivery>> captor = ArgumentCaptor.captor();
                then(notificationDeliveryRepository).should().saveAll(captor.capture());
                assertThat(captor.getValue())
                        .filteredOn(delivery -> delivery.getType() == NotificationType.REMINDER)
                        .extracting(
                                NotificationDelivery::getGatheringId,
                                NotificationDelivery::getParticipantId,
                                NotificationDelivery::getSettlementIds,
                                NotificationDelivery::getStatus,
                                NotificationDelivery::getAvailableAt)
                        .containsExactly(
                                tuple(
                                        GATHERING_ID,
                                        PARK_ID,
                                        List.of(SETTLEMENT_ID, SECOND_SETTLEMENT_ID),
                                        DeliveryStatus.PENDING,
                                        THREE_DAYS_LATER),
                                tuple(
                                        GATHERING_ID,
                                        PARK_ID,
                                        List.of(SETTLEMENT_ID, SECOND_SETTLEMENT_ID),
                                        DeliveryStatus.PENDING,
                                        FIVE_DAYS_LATER),
                                tuple(
                                        GATHERING_ID,
                                        PAYER_ID,
                                        List.of(THIRD_SETTLEMENT_ID),
                                        DeliveryStatus.PENDING,
                                        THREE_DAYS_LATER),
                                tuple(
                                        GATHERING_ID,
                                        PAYER_ID,
                                        List.of(THIRD_SETTLEMENT_ID),
                                        DeliveryStatus.PENDING,
                                        FIVE_DAYS_LATER));
            }

            @Test
            @DisplayName(
                    "같은 참여자의 보내기를 기다리는 리마인드가 3일 뒤 시각보다 24시간에서 1초 모자란 시각에 있으면 3일 뒤 리마인드를 만들지 않는다")
            void shouldNotSaveReminderWhenPendingReminderWithinGap() {
                // given
                givenDueSchedule(NOW);
                givenUnnotifiedSettlements();
                givenReminders(
                        reminderOf(PARK_ID)
                                .availableAt(THREE_DAYS_LATER.minusHours(24).plusSeconds(1))
                                .build());

                // when
                dispatchSettlementNotificationService.dispatch(GATHERING_ID);

                // then
                thenRemindersOfParkSavedAt(FIVE_DAYS_LATER);
            }

            @Test
            @DisplayName("같은 참여자의 보내기를 기다리는 리마인드가 3일 뒤 시각보다 정확히 24시간 전에 있으면 3일 뒤 리마인드를 만든다")
            void shouldSaveReminderWhenPendingReminderAtGap() {
                // given
                givenDueSchedule(NOW);
                givenUnnotifiedSettlements();
                givenReminders(
                        reminderOf(PARK_ID).availableAt(THREE_DAYS_LATER.minusHours(24)).build());

                // when
                dispatchSettlementNotificationService.dispatch(GATHERING_ID);

                // then
                thenRemindersOfParkSavedAt(THREE_DAYS_LATER, FIVE_DAYS_LATER);
            }

            @Test
            @DisplayName("같은 참여자의 리마인드가 같은 시각에 있어도 건너뛴 리마인드면 리마인드를 만든다")
            void shouldSaveReminderWhenSkippedReminderWithinGap() {
                // given
                givenDueSchedule(NOW);
                givenUnnotifiedSettlements();
                givenReminders(
                        reminderOf(PARK_ID)
                                .availableAt(THREE_DAYS_LATER)
                                .status(DeliveryStatus.SKIPPED)
                                .build());

                // when
                dispatchSettlementNotificationService.dispatch(GATHERING_ID);

                // then
                thenRemindersOfParkSavedAt(THREE_DAYS_LATER, FIVE_DAYS_LATER);
            }

            @Test
            @DisplayName("다른 참여자의 리마인드가 같은 시각에 있어도 리마인드를 만든다")
            void shouldSaveReminderWhenOtherParticipantReminderWithinGap() {
                // given
                givenDueSchedule(NOW);
                givenUnnotifiedSettlements();
                givenReminders(reminderOf(LEE_ID).availableAt(THREE_DAYS_LATER).build());

                // when
                dispatchSettlementNotificationService.dispatch(GATHERING_ID);

                // then
                thenRemindersOfParkSavedAt(THREE_DAYS_LATER, FIVE_DAYS_LATER);
            }

            @Test
            @DisplayName("예약 뒤에 정산 건이 모두 취소됐으면 발송 기록 없이 예약 시각만 비워 저장한다")
            void shouldClearScheduleWithoutNoticesWhenSettlementsCancelled() {
                // given
                NotificationSchedule schedule = givenDueSchedule(NOW);
                given(settlementRepository.findAllUnnotifiedByGatheringIdForUpdate(GATHERING_ID))
                        .willReturn(List.of());

                // when
                dispatchSettlementNotificationService.dispatch(GATHERING_ID);

                // then
                then(notificationDeliveryRepository).should().saveAll(List.of());
                then(notificationScheduleRepository).should().save(schedule);
                assertThat(schedule.isScheduled()).isFalse();
            }

            // 이는 1차에서 보냄, 2차에서 0원이고 3차에서 확인 완료라 받을 메시지가 없다.
            // 박은 1차와 2차의 미입금 분담금을 한 메시지로 받고, 결제자는 박이 낸 3차의 미입금 분담금을 받는다.
            private List<Settlement> givenUnnotifiedSettlements() {
                List<Settlement> settlements =
                        List.of(
                                SettlementFixture.DINNER.createWithFirstShareStatus(
                                        SETTLEMENT_ID,
                                        GATHERING_ID,
                                        PAYER_ID,
                                        PARTICIPANT_IDS,
                                        ShareStatus.SENT),
                                SettlementFixture.SECOND_ROUND.createWithFixedAmounts(
                                        SECOND_SETTLEMENT_ID,
                                        GATHERING_ID,
                                        PAYER_ID,
                                        PARTICIPANT_IDS,
                                        Map.of(LEE_ID, 0L)),
                                SettlementFixture.DINNER.createWithFirstShareStatus(
                                        THIRD_SETTLEMENT_ID,
                                        GATHERING_ID,
                                        PARK_ID,
                                        List.of(PARK_ID, LEE_ID, PAYER_ID),
                                        ShareStatus.CONFIRMED));
                given(settlementRepository.findAllUnnotifiedByGatheringIdForUpdate(GATHERING_ID))
                        .willReturn(settlements);
                return settlements;
            }

            private NotificationDelivery.NotificationDeliveryBuilder reminderOf(
                    Long participantId) {
                return NotificationDeliveryFixture.REMINDER_NOTICE.builder(
                        1L, GATHERING_ID, participantId, List.of(SETTLEMENT_ID));
            }

            private void givenReminders(NotificationDelivery... reminders) {
                given(notificationDeliveryRepository.findAllRemindersByGatheringId(GATHERING_ID))
                        .willReturn(List.of(reminders));
            }

            private void thenRemindersOfParkSavedAt(LocalDateTime... availableAts) {
                ArgumentCaptor<List<NotificationDelivery>> captor = ArgumentCaptor.captor();
                then(notificationDeliveryRepository).should().saveAll(captor.capture());
                assertThat(captor.getValue())
                        .filteredOn(
                                delivery ->
                                        delivery.getType() == NotificationType.REMINDER
                                                && delivery.getParticipantId().equals(PARK_ID))
                        .extracting(NotificationDelivery::getAvailableAt)
                        .containsExactly(availableAts);
            }
        }

        @Nested
        @DisplayName("보낼 수 없는 시간대에 발송을 시작하게 된 경우")
        class WhenNotSendable {

            @Test
            @DisplayName("22:00이면 정산 건을 건드리지 않고 예약을 다음 날 10:00로 미뤄 저장한다")
            void shouldDeferScheduleToNextDayWhenSendingEnded() {
                // given
                LocalDateTime now = LocalDateTime.of(2026, 10, 28, 22, 0);
                givenNow(now);
                NotificationSchedule schedule = givenDueSchedule(now);

                // when
                dispatchSettlementNotificationService.dispatch(GATHERING_ID);

                // then
                then(settlementRepository).shouldHaveNoInteractions();
                then(notificationDeliveryRepository).shouldHaveNoInteractions();
                then(notificationScheduleRepository).should().save(schedule);
                assertThat(schedule.getScheduledAt())
                        .isEqualTo(LocalDateTime.of(2026, 10, 29, 10, 0));
            }

            @Test
            @DisplayName("09:59이면 정산 건을 건드리지 않고 예약을 같은 날 10:00로 미뤄 저장한다")
            void shouldDeferScheduleToSameDayWhenBeforeSendingStart() {
                // given
                LocalDateTime now = LocalDateTime.of(2026, 10, 28, 9, 59);
                givenNow(now);
                NotificationSchedule schedule = givenDueSchedule(now);

                // when
                dispatchSettlementNotificationService.dispatch(GATHERING_ID);

                // then
                then(settlementRepository).shouldHaveNoInteractions();
                then(notificationDeliveryRepository).shouldHaveNoInteractions();
                then(notificationScheduleRepository).should().save(schedule);
                assertThat(schedule.getScheduledAt())
                        .isEqualTo(LocalDateTime.of(2026, 10, 28, 10, 0));
            }
        }

        // 장애로 늦게 발송을 시작한 경우를 흉내 내려고 예약 시각을 현재보다 1시간 앞에 둔다.
        private NotificationSchedule givenDueSchedule(LocalDateTime now) {
            NotificationSchedule schedule =
                    NotificationSchedule.builder()
                            .gatheringId(GATHERING_ID)
                            .scheduledAt(now.minusHours(1))
                            .build();
            given(notificationScheduleRepository.findDueForUpdate(GATHERING_ID, now))
                    .willReturn(Optional.of(schedule));
            return schedule;
        }
    }

    private void givenNow(LocalDateTime now) {
        dispatchSettlementNotificationService =
                new DispatchSettlementNotificationService(
                        notificationScheduleRepository,
                        new NotificationTargetProviderImpl(settlementRepository),
                        notificationDeliveryRepository,
                        Clock.fixed(
                                now.atZone(TimeConfig.SERVICE_ZONE).toInstant(),
                                TimeConfig.SERVICE_ZONE));
    }
}
