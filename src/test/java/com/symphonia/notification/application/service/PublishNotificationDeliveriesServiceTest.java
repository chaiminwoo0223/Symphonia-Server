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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@DisplayName("PublishNotificationDeliveriesService 단위 테스트")
class PublishNotificationDeliveriesServiceTest extends UnitTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 15, 0);
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

    private List<NotificationDelivery> unpublishedDeliveries() {
        return UNPUBLISHED_IDS.stream().map(id -> delivery(id).build()).toList();
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
