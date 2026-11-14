package com.symphonia.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.global.config.TimeConfig;
import com.symphonia.notification.domain.entity.NotificationDelivery;
import com.symphonia.notification.domain.publisher.NotificationDeliveryPublisher;
import com.symphonia.notification.domain.repository.NotificationDeliveryRepository;
import com.symphonia.notification.domain.vo.DeliveryStatus;
import com.symphonia.notification.fixture.NotificationDeliveryFixture;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@DisplayName("PublishNotificationDeliveriesService 단위 테스트")
class PublishNotificationDeliveriesServiceTest extends UnitTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 15, 0);
    private static final int PUBLISH_LIMIT = 100;
    private static final int MAX_IN_FLIGHT = 200;
    private static final Long GATHERING_ID = 1L;
    private static final Long PARTICIPANT_ID = 2L;
    private static final Long SETTLEMENT_ID = 3L;
    private static final Long FIRST_DELIVERY_ID = 1L;
    private static final Long SECOND_DELIVERY_ID = 2L;
    private static final Long THIRD_DELIVERY_ID = 3L;
    private static final List<Long> UNPUBLISHED_IDS =
            List.of(FIRST_DELIVERY_ID, SECOND_DELIVERY_ID, THIRD_DELIVERY_ID);

    private PublishNotificationDeliveriesService publishNotificationDeliveriesService;

    @Mock private NotificationDeliveryRepository notificationDeliveryRepository;
    @Mock private NotificationDeliveryPublisher notificationDeliveryPublisher;

    @BeforeEach
    void setUp() {
        publishNotificationDeliveriesService =
                new PublishNotificationDeliveriesService(
                        notificationDeliveryRepository,
                        notificationDeliveryPublisher,
                        Clock.fixed(
                                NOW.atZone(TimeConfig.SERVICE_ZONE).toInstant(),
                                TimeConfig.SERVICE_ZONE));
    }

    @Nested
    @DisplayName("publishPending 메서드는")
    class PublishPending {

        @Nested
        @DisplayName("아직 발행하지 않은 발송 기록만 있고 브로커가 모두 받았다고 확인한 경우")
        class WhenAllConfirmed {

            @Test
            @DisplayName("현재 시각, 5분 전 재발행 기준, 5분 전 맡은 시각 만료 기준, 100건으로 찾은 발송 기록을 발행하고 모두 발행했다고 남긴다")
            void shouldMarkAllPublishedWhenAllConfirmed() {
                // given
                givenPublishable(unpublishedDeliveries());
                given(notificationDeliveryPublisher.publishAll(UNPUBLISHED_IDS))
                        .willReturn(UNPUBLISHED_IDS);

                // when
                publishNotificationDeliveriesService.publishPending();

                // then
                then(notificationDeliveryRepository).should().markPublished(UNPUBLISHED_IDS, NOW);
                then(notificationDeliveryRepository).should().saveAll(List.of());
            }
        }

        @Nested
        @DisplayName("아직 발행하지 않은 발송 기록만 있고 브로커가 일부만 받았다고 확인한 경우")
        class WhenPartiallyConfirmed {

            @Test
            @DisplayName("브로커가 확인한 발송 기록만 발행했다고 남긴다")
            void shouldMarkOnlyConfirmedPublishedWhenPartiallyConfirmed() {
                // given
                givenPublishable(unpublishedDeliveries());
                given(notificationDeliveryPublisher.publishAll(UNPUBLISHED_IDS))
                        .willReturn(List.of(FIRST_DELIVERY_ID, THIRD_DELIVERY_ID));

                // when
                publishNotificationDeliveriesService.publishPending();

                // then
                then(notificationDeliveryRepository)
                        .should()
                        .markPublished(List.of(FIRST_DELIVERY_ID, THIRD_DELIVERY_ID), NOW);
            }
        }

        @Nested
        @DisplayName("발행했는데 보내지 못한 발송 기록이 섞인 경우")
        class WhenStalledDeliveryIncluded {

            @Test
            @DisplayName(
                    "발행했는데 보내지 못한 발송 기록은 다시 발행하지 않고 시도 한 번으로 세어 1분 뒤로 미루고, 발행하지 않은 발송 기록만 발행한다")
            void shouldRetryStalledLaterAndPublishOnlyUnpublishedWhenStalledDeliveryIncluded() {
                // given
                NotificationDelivery stalled =
                        delivery(SECOND_DELIVERY_ID)
                                .publishedAt(NOW.minusMinutes(10))
                                .claimedAt(NOW.minusMinutes(6))
                                .build();
                givenPublishable(
                        List.of(
                                delivery(FIRST_DELIVERY_ID).build(),
                                stalled,
                                delivery(THIRD_DELIVERY_ID).build()));
                List<Long> unpublishedIds = List.of(FIRST_DELIVERY_ID, THIRD_DELIVERY_ID);
                given(notificationDeliveryPublisher.publishAll(unpublishedIds))
                        .willReturn(unpublishedIds);

                // when
                publishNotificationDeliveriesService.publishPending();

                // then
                assertThat(savedStalledDeliveries())
                        .extracting(
                                NotificationDelivery::getId,
                                NotificationDelivery::getStatus,
                                NotificationDelivery::getAttempts,
                                NotificationDelivery::getAvailableAt,
                                NotificationDelivery::getPublishedAt,
                                NotificationDelivery::getClaimedAt)
                        .containsExactly(
                                tuple(
                                        SECOND_DELIVERY_ID,
                                        DeliveryStatus.PENDING,
                                        1,
                                        NOW.plusMinutes(1),
                                        null,
                                        null));
                then(notificationDeliveryRepository).should().markPublished(unpublishedIds, NOW);
            }
        }

        @Nested
        @DisplayName("상한만큼 찾은 발송 기록을 브로커가 모두 받았다고 확인한 경우")
        class WhenFullBatchPublished {

            @Test
            @DisplayName("남은 발송 기록이 있을 수 있으므로 true를 반환한다")
            void shouldReturnTrueWhenFullBatchPublished() {
                // given
                List<NotificationDelivery> deliveries = unpublishedDeliveries(PUBLISH_LIMIT);
                List<Long> ids = idsOf(deliveries);
                givenPublishable(deliveries);
                given(notificationDeliveryPublisher.publishAll(ids)).willReturn(ids);

                // when
                boolean result = publishNotificationDeliveriesService.publishPending();

                // then
                assertThat(result).isTrue();
            }
        }

        @Nested
        @DisplayName("상한보다 적게 찾은 경우")
        class WhenFewerThanLimitFound {

            @Test
            @DisplayName("남은 발송 기록이 없으므로 false를 반환한다")
            void shouldReturnFalseWhenFewerThanLimitFound() {
                // given
                givenPublishable(unpublishedDeliveries());
                given(notificationDeliveryPublisher.publishAll(UNPUBLISHED_IDS))
                        .willReturn(UNPUBLISHED_IDS);

                // when
                boolean result = publishNotificationDeliveriesService.publishPending();

                // then
                assertThat(result).isFalse();
            }
        }

        @Nested
        @DisplayName("상한만큼 찾았지만 브로커가 일부만 받았다고 확인한 경우")
        class WhenFullBatchPartiallyConfirmed {

            @Test
            @DisplayName("바로 다시 부르면 같은 발송 기록을 조회하므로 false를 반환한다")
            void shouldReturnFalseWhenFullBatchPartiallyConfirmed() {
                // given
                List<NotificationDelivery> deliveries = unpublishedDeliveries(PUBLISH_LIMIT);
                List<Long> ids = idsOf(deliveries);
                givenPublishable(deliveries);
                given(notificationDeliveryPublisher.publishAll(ids))
                        .willReturn(ids.subList(0, PUBLISH_LIMIT - 1));

                // when
                boolean result = publishNotificationDeliveriesService.publishPending();

                // then
                assertThat(result).isFalse();
            }
        }

        @Nested
        @DisplayName("발행했지만 아직 보내지 못한 발송 기록이 상한에 가까운 경우")
        class WhenInFlightNearLimit {

            @Test
            @DisplayName("상한까지 남은 수만큼만 발행하고 false를 반환한다")
            void shouldPublishOnlyRemainingCapacityAndReturnFalseWhenInFlightNearLimit() {
                // given
                List<NotificationDelivery> deliveries = unpublishedDeliveries(PUBLISH_LIMIT);
                List<Long> publishableIds = idsOf(deliveries).subList(0, 50);
                givenPublishable(deliveries);
                givenInFlight(MAX_IN_FLIGHT - 50);
                given(notificationDeliveryPublisher.publishAll(publishableIds))
                        .willReturn(publishableIds);

                // when
                boolean result = publishNotificationDeliveriesService.publishPending();

                // then
                assertThat(result).isFalse();
                then(notificationDeliveryRepository).should().markPublished(publishableIds, NOW);
            }
        }

        @Nested
        @DisplayName("발행했지만 아직 보내지 못한 발송 기록이 상한에 이른 경우")
        class WhenInFlightReachedLimit {

            @Test
            @DisplayName("새로 발행하지 않지만 발행했는데 보내지 못한 발송 기록은 시도 한 번으로 센다")
            void shouldNotPublishButStillRetryStalledWhenInFlightReachedLimit() {
                // given
                NotificationDelivery stalled =
                        delivery(SECOND_DELIVERY_ID)
                                .publishedAt(NOW.minusMinutes(10))
                                .claimedAt(NOW.minusMinutes(6))
                                .build();
                givenPublishable(List.of(delivery(FIRST_DELIVERY_ID).build(), stalled));
                givenInFlight(MAX_IN_FLIGHT);
                given(notificationDeliveryPublisher.publishAll(List.of())).willReturn(List.of());

                // when
                boolean result = publishNotificationDeliveriesService.publishPending();

                // then
                assertThat(result).isFalse();
                assertThat(savedStalledDeliveries())
                        .extracting(NotificationDelivery::getId, NotificationDelivery::getAttempts)
                        .containsExactly(tuple(SECOND_DELIVERY_ID, 1));
                then(notificationDeliveryRepository).should().markPublished(List.of(), NOW);
            }
        }

        @Nested
        @DisplayName("발행했는데 보내지 못한 발송 기록이 재시도를 모두 쓴 경우")
        class WhenStalledDeliveryRetriesExhausted {

            @Test
            @DisplayName("다시 발행하지 않고 FAILED로 끝낸다")
            void shouldFailStalledWhenStalledDeliveryRetriesExhausted() {
                // given
                givenPublishable(
                        List.of(
                                delivery(FIRST_DELIVERY_ID)
                                        .attempts(4)
                                        .publishedAt(NOW.minusMinutes(10))
                                        .build()));
                given(notificationDeliveryPublisher.publishAll(List.of())).willReturn(List.of());

                // when
                publishNotificationDeliveriesService.publishPending();

                // then
                assertThat(savedStalledDeliveries())
                        .extracting(NotificationDelivery::getId, NotificationDelivery::getStatus)
                        .containsExactly(tuple(FIRST_DELIVERY_ID, DeliveryStatus.FAILED));
                then(notificationDeliveryRepository).should().markPublished(List.of(), NOW);
            }
        }
    }

    private void givenPublishable(List<NotificationDelivery> notificationDeliveries) {
        given(
                        notificationDeliveryRepository.findPublishableForUpdate(
                                NOW, NOW.minusMinutes(5), NOW.minusMinutes(5), 100))
                .willReturn(notificationDeliveries);
    }

    private void givenInFlight(long inFlight) {
        given(notificationDeliveryRepository.countInFlight(NOW.minusMinutes(5)))
                .willReturn(inFlight);
    }

    private List<NotificationDelivery> unpublishedDeliveries() {
        return UNPUBLISHED_IDS.stream().map(id -> delivery(id).build()).toList();
    }

    private List<NotificationDelivery> unpublishedDeliveries(int count) {
        return LongStream.rangeClosed(1, count).mapToObj(id -> delivery(id).build()).toList();
    }

    private List<Long> idsOf(List<NotificationDelivery> notificationDeliveries) {
        return notificationDeliveries.stream().map(NotificationDelivery::getId).toList();
    }

    private NotificationDelivery.NotificationDeliveryBuilder delivery(Long deliveryId) {
        return NotificationDeliveryFixture.SETTLEMENT_NOTICE.builder(
                deliveryId, GATHERING_ID, PARTICIPANT_ID, List.of(SETTLEMENT_ID));
    }

    private List<NotificationDelivery> savedStalledDeliveries() {
        ArgumentCaptor<List<NotificationDelivery>> captor = ArgumentCaptor.captor();
        then(notificationDeliveryRepository).should().saveAll(captor.capture());
        return captor.getValue();
    }
}
