package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.result.PreparedNotificationResult;
import com.symphonia.gathering.application.dto.result.PreparedNotificationResult.PayerAmountResult;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.NotificationDelivery;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.NotificationDeliveryRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.DeliveryStatus;
import com.symphonia.gathering.domain.vo.NotificationType;
import com.symphonia.gathering.domain.vo.ShareStatus;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.NotificationDeliveryFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.global.config.TimeConfig;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@DisplayName("PrepareNotificationService 단위 테스트")
class PrepareNotificationServiceTest extends UnitTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 15, 0);
    private static final LocalDateTime GATHERING_OPENED_AT = LocalDateTime.of(2026, 10, 27, 18, 0);
    private static final Long DELIVERY_ID = 40L;
    private static final Long GATHERING_ID = 10L;
    private static final Long SETTLEMENT_ID = 30L;
    private static final Long SECOND_SETTLEMENT_ID = 31L;
    private static final Long THIRD_SETTLEMENT_ID = 32L;
    private static final Long PAYER_ID = 20L;
    private static final Long LEE_ID = 21L;
    private static final Long PARK_ID = 22L;
    private static final List<Long> PARTICIPANT_IDS = List.of(PAYER_ID, LEE_ID, PARK_ID);
    private static final String LINK_TOKEN = "new-link-token";

    private PrepareNotificationService prepareNotificationService;

    @Mock private NotificationDeliveryRepository notificationDeliveryRepository;
    @Mock private GatheringRepository gatheringRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private ParticipantLinkTokenRepository participantLinkTokenRepository;
    @Mock private GatheringTokenProvider gatheringTokenProvider;

    @BeforeEach
    void setUp() {
        givenNow(NOW);
    }

    @Nested
    @DisplayName("prepare 메서드는")
    class Prepare {

        @Nested
        @DisplayName("발송 기록이 없는 경우")
        class WhenDeliveryNotFound {

            @Test
            @DisplayName("보낼 메시지 없이 빈 값을 반환한다")
            void shouldReturnEmpty() {
                // given
                given(notificationDeliveryRepository.findByIdForUpdate(DELIVERY_ID))
                        .willReturn(Optional.empty());

                // when
                Optional<PreparedNotificationResult> result =
                        prepareNotificationService.prepare(DELIVERY_ID);

                // then
                assertThat(result).isEmpty();
                then(gatheringRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("맡을 수 없는 발송 기록인 경우")
        class WhenNotClaimable {

            @Test
            @DisplayName("이미 보낸 발송 기록이면 저장하지 않고 빈 값을 반환한다")
            void shouldReturnEmptyWhenDeliveryNotPending() {
                // given
                givenDelivery(
                        settlementNoticeBuilder(PARK_ID, List.of(SETTLEMENT_ID))
                                .status(DeliveryStatus.SENT)
                                .build());

                // when
                Optional<PreparedNotificationResult> result =
                        prepareNotificationService.prepare(DELIVERY_ID);

                // then
                assertThat(result).isEmpty();
                thenNothingPrepared();
            }

            @Test
            @DisplayName("보낼 수 있는 시각이 아직 오지 않았으면 저장하지 않고 빈 값을 반환한다")
            void shouldReturnEmptyWhenNotAvailableYet() {
                // given
                givenDelivery(
                        settlementNoticeBuilder(PARK_ID, List.of(SETTLEMENT_ID))
                                .availableAt(NOW.plusMinutes(1))
                                .build());

                // when
                Optional<PreparedNotificationResult> result =
                        prepareNotificationService.prepare(DELIVERY_ID);

                // then
                assertThat(result).isEmpty();
                thenNothingPrepared();
            }

            @Test
            @DisplayName("다른 컨슈머가 맡은 지 5분이 지나지 않았으면 저장하지 않고 빈 값을 반환한다")
            void shouldReturnEmptyWhenClaimedWithinLease() {
                // given
                givenDelivery(
                        settlementNoticeBuilder(PARK_ID, List.of(SETTLEMENT_ID))
                                .claimedAt(NOW.minusMinutes(4))
                                .build());

                // when
                Optional<PreparedNotificationResult> result =
                        prepareNotificationService.prepare(DELIVERY_ID);

                // then
                assertThat(result).isEmpty();
                thenNothingPrepared();
            }

            private void thenNothingPrepared() {
                then(notificationDeliveryRepository)
                        .should(never())
                        .save(any(NotificationDelivery.class));
                then(gatheringRepository).shouldHaveNoInteractions();
                then(gatheringTokenProvider).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("보낼 수 없는 시간대에 맡게 된 경우")
        class WhenNotSendable {

            @Test
            @DisplayName("다음 날 10:00로 미루고 발행 시각과 맡은 시각을 비워 저장한 뒤 빈 값을 반환한다")
            void shouldDeferDeliveryToNextSendingStart() {
                // given
                LocalDateTime now = LocalDateTime.of(2026, 10, 28, 22, 0);
                givenNow(now);
                NotificationDelivery delivery =
                        settlementNoticeBuilder(PARK_ID, List.of(SETTLEMENT_ID))
                                .publishedAt(now.minusMinutes(10))
                                .claimedAt(now.minusMinutes(10))
                                .build();
                givenDelivery(delivery);

                // when
                Optional<PreparedNotificationResult> result =
                        prepareNotificationService.prepare(DELIVERY_ID);

                // then
                assertThat(result).isEmpty();
                then(notificationDeliveryRepository).should().save(delivery);
                assertThat(delivery.getAvailableAt())
                        .isEqualTo(LocalDateTime.of(2026, 10, 29, 10, 0));
                assertThat(delivery.getPublishedAt()).isNull();
                assertThat(delivery.getClaimedAt()).isNull();
                assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
                then(gatheringRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임이 없는 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("GatheringNotFoundException을 던지고 발송 기록을 저장하지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                givenDelivery(settlementNoticeBuilder(PARK_ID, List.of(SETTLEMENT_ID)).build());
                givenSettlements(
                        List.of(SETTLEMENT_ID),
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));
                given(gatheringRepository.findById(GATHERING_ID)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> prepareNotificationService.prepare(DELIVERY_ID))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(notificationDeliveryRepository)
                        .should(never())
                        .save(any(NotificationDelivery.class));
            }
        }

        @Nested
        @DisplayName("알릴 금액이 없는 경우")
        class WhenNothingToAnnounce {

            @Test
            @DisplayName("수신자가 그사이 분담금을 보냈으면 보내지 않음으로 바꿔 저장하고 빈 값을 반환한다")
            void shouldSkipWhenShareAlreadySent() {
                // given
                NotificationDelivery delivery =
                        settlementNoticeBuilder(LEE_ID, List.of(SETTLEMENT_ID)).build();
                givenDelivery(delivery);
                givenSettlements(
                        List.of(SETTLEMENT_ID),
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));

                // when
                Optional<PreparedNotificationResult> result =
                        prepareNotificationService.prepare(DELIVERY_ID);

                // then
                assertThat(result).isEmpty();
                thenSkipped(delivery);
            }

            @Test
            @DisplayName("정산 메시지에 담을 정산 건이 그사이 취소됐으면 보내지 않음으로 바꿔 저장하고 빈 값을 반환한다")
            void shouldSkipWhenSettlementCancelled() {
                // given
                NotificationDelivery delivery =
                        settlementNoticeBuilder(LEE_ID, List.of(SETTLEMENT_ID)).build();
                givenDelivery(delivery);
                givenSettlements(
                        List.of(SETTLEMENT_ID),
                        SettlementFixture.DINNER.createCancelled(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));

                // when
                Optional<PreparedNotificationResult> result =
                        prepareNotificationService.prepare(DELIVERY_ID);

                // then
                assertThat(result).isEmpty();
                thenSkipped(delivery);
            }

            private void thenSkipped(NotificationDelivery delivery) {
                then(notificationDeliveryRepository).should().save(delivery);
                assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SKIPPED);
                assertThat(delivery.getClaimedAt()).isNull();
                then(gatheringRepository).shouldHaveNoInteractions();
                then(gatheringTokenProvider).shouldHaveNoInteractions();
                then(participantLinkTokenRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("정산 메시지를 보낼 수 있는 경우")
        class WhenSettlementNoticePreparable {

            @Test
            @DisplayName("같은 결제자의 미입금 분담금은 합하고 결제자가 다르면 따로 담아, 수신자와 모임 정보와 함께 반환한다")
            void shouldReturnPreparedNotificationWithPayerAmountsSummedPerPayer() {
                // given
                givenPreparableSettlementNotice(
                        settlementNoticeBuilder(PARK_ID, settlementIdsOfPark()).build());

                // when
                Optional<PreparedNotificationResult> result =
                        prepareNotificationService.prepare(DELIVERY_ID);

                // then
                assertThat(result).isPresent();
                PreparedNotificationResult prepared = result.get();
                assertThat(prepared.notificationDeliveryId()).isEqualTo(DELIVERY_ID);
                assertThat(prepared.type()).isEqualTo(NotificationType.SETTLEMENT);
                assertThat(prepared.recipientPhoneNumber())
                        .isEqualTo(ParticipantFixture.PARK.getPhoneNumber());
                assertThat(prepared.recipientName()).isEqualTo(ParticipantFixture.PARK.getName());
                assertThat(prepared.gatheringName())
                        .isEqualTo(GatheringFixture.FRIDAY_DINNER.getName());
                assertThat(prepared.gatheringDate()).isEqualTo(GATHERING_OPENED_AT.toLocalDate());
                assertThat(prepared.payerAmounts())
                        .extracting(PayerAmountResult::payerName, PayerAmountResult::amount)
                        .containsExactly(
                                tuple(ParticipantFixture.KIM.getName(), 9_999L),
                                tuple(ParticipantFixture.LEE.getName(), 5_000L));
            }

            @Test
            @DisplayName("맡은 시각을 남겨 저장하고, 수신자의 새 개인 링크 토큰을 발급해 저장한 뒤 메시지에 담는다")
            void shouldClaimDeliveryAndIssueLinkToken() {
                // given
                NotificationDelivery delivery =
                        settlementNoticeBuilder(PARK_ID, settlementIdsOfPark()).build();
                givenPreparableSettlementNotice(delivery);

                // when
                Optional<PreparedNotificationResult> result =
                        prepareNotificationService.prepare(DELIVERY_ID);

                // then
                then(notificationDeliveryRepository).should().save(delivery);
                assertThat(delivery.getClaimedAt()).isEqualTo(NOW);
                assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
                then(participantLinkTokenRepository).should().save(LINK_TOKEN, PARK_ID);
                assertThat(result)
                        .get()
                        .extracting(PreparedNotificationResult::linkToken)
                        .isEqualTo(LINK_TOKEN);
            }

            @Test
            @DisplayName("다른 컨슈머가 맡은 지 5분이 지났으면 다시 맡아 메시지를 준비한다")
            void shouldClaimAgainWhenClaimLeaseExpired() {
                // given
                NotificationDelivery delivery =
                        settlementNoticeBuilder(PARK_ID, settlementIdsOfPark())
                                .claimedAt(NOW.minusMinutes(6))
                                .build();
                givenPreparableSettlementNotice(delivery);

                // when
                Optional<PreparedNotificationResult> result =
                        prepareNotificationService.prepare(DELIVERY_ID);

                // then
                assertThat(result).isPresent();
                assertThat(delivery.getClaimedAt()).isEqualTo(NOW);
            }

            // 박은 결제자에게 1차 3,333원과 2차 6,666원을, 이에게 3차 5,000원을 낸다.
            private void givenPreparableSettlementNotice(NotificationDelivery delivery) {
                givenDelivery(delivery);
                givenGathering();
                givenSettlements(
                        settlementIdsOfPark(),
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS),
                        SettlementFixture.SECOND_ROUND.createWithId(
                                SECOND_SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS),
                        SettlementFixture.DINNER.createWithId(
                                THIRD_SETTLEMENT_ID,
                                GATHERING_ID,
                                LEE_ID,
                                List.of(LEE_ID, PARK_ID)));
                givenClaimSaved();
                givenLinkTokenGenerated();
            }

            private List<Long> settlementIdsOfPark() {
                return List.of(SETTLEMENT_ID, SECOND_SETTLEMENT_ID, THIRD_SETTLEMENT_ID);
            }
        }

        @Nested
        @DisplayName("취소 알림을 보낼 수 있는 경우")
        class WhenCancellationNoticePreparable {

            @Test
            @DisplayName("취소된 정산 건의 분담금을 결제자 이름과 함께 담아 반환한다")
            void shouldReturnPreparedNotificationWithCancelledShare() {
                // given
                givenDelivery(
                        NotificationDeliveryFixture.CANCELLATION_NOTICE
                                .builder(DELIVERY_ID, GATHERING_ID, LEE_ID, List.of(SETTLEMENT_ID))
                                .build());
                givenGathering();
                givenSettlements(
                        List.of(SETTLEMENT_ID),
                        SettlementFixture.DINNER.createCancelled(
                                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS));
                givenClaimSaved();
                givenLinkTokenGenerated();

                // when
                Optional<PreparedNotificationResult> result =
                        prepareNotificationService.prepare(DELIVERY_ID);

                // then
                assertThat(result).isPresent();
                PreparedNotificationResult prepared = result.get();
                assertThat(prepared.type()).isEqualTo(NotificationType.CANCELLATION);
                assertThat(prepared.recipientName()).isEqualTo(ParticipantFixture.LEE.getName());
                assertThat(prepared.payerAmounts())
                        .extracting(PayerAmountResult::payerName, PayerAmountResult::amount)
                        .containsExactly(tuple(ParticipantFixture.KIM.getName(), 3_333L));
            }
        }

        private NotificationDelivery.NotificationDeliveryBuilder settlementNoticeBuilder(
                Long participantId, List<Long> settlementIds) {
            return NotificationDeliveryFixture.SETTLEMENT_NOTICE.builder(
                    DELIVERY_ID, GATHERING_ID, participantId, settlementIds);
        }

        private void givenDelivery(NotificationDelivery delivery) {
            given(notificationDeliveryRepository.findByIdForUpdate(DELIVERY_ID))
                    .willReturn(Optional.of(delivery));
        }

        private void givenGathering() {
            given(gatheringRepository.findById(GATHERING_ID)).willReturn(Optional.of(gathering()));
        }

        private void givenSettlements(List<Long> settlementIds, Settlement... settlements) {
            given(settlementRepository.findAllByIds(settlementIds))
                    .willReturn(List.of(settlements));
        }

        private void givenClaimSaved() {
            given(notificationDeliveryRepository.save(any(NotificationDelivery.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
        }

        private void givenLinkTokenGenerated() {
            given(gatheringTokenProvider.generate()).willReturn(LINK_TOKEN);
        }
    }

    private Gathering gathering() {
        return GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                GATHERING_ID,
                GATHERING_OPENED_AT,
                List.of(
                        ParticipantFixture.KIM.createWithId(PAYER_ID),
                        ParticipantFixture.LEE.createWithId(LEE_ID),
                        ParticipantFixture.PARK.createWithId(PARK_ID)));
    }

    private void givenNow(LocalDateTime now) {
        prepareNotificationService =
                new PrepareNotificationService(
                        notificationDeliveryRepository,
                        gatheringRepository,
                        settlementRepository,
                        participantLinkTokenRepository,
                        gatheringTokenProvider,
                        Clock.fixed(
                                now.atZone(TimeConfig.SERVICE_ZONE).toInstant(),
                                TimeConfig.SERVICE_ZONE));
    }
}
