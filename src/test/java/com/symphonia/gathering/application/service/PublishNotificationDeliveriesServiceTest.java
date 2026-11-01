package com.symphonia.gathering.application.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.gathering.domain.publisher.NotificationDeliveryPublisher;
import com.symphonia.gathering.domain.repository.NotificationDeliveryRepository;
import com.symphonia.global.config.TimeConfig;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@DisplayName("PublishNotificationDeliveriesService 단위 테스트")
class PublishNotificationDeliveriesServiceTest extends UnitTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 15, 0);
    private static final Long FIRST_DELIVERY_ID = 1L;
    private static final Long SECOND_DELIVERY_ID = 2L;
    private static final Long THIRD_DELIVERY_ID = 3L;
    private static final List<Long> PUBLISHABLE_IDS =
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

        @BeforeEach
        void setUp() {
            given(
                            notificationDeliveryRepository.findPublishableIdsForUpdate(
                                    NOW, NOW.minusMinutes(5), NOW.minusMinutes(5), 100))
                    .willReturn(PUBLISHABLE_IDS);
        }

        @Nested
        @DisplayName("브로커가 모든 발송 기록을 받았다고 확인한 경우")
        class WhenAllConfirmed {

            @Test
            @DisplayName("현재 시각, 5분 전 재발행 기준, 5분 전 맡은 시각 만료 기준, 100건으로 찾은 발송 기록을 발행하고 모두 발행했다고 남긴다")
            void shouldMarkAllPublishedWhenAllConfirmed() {
                // given
                given(notificationDeliveryPublisher.publishAll(PUBLISHABLE_IDS))
                        .willReturn(PUBLISHABLE_IDS);

                // when
                publishNotificationDeliveriesService.publishPending();

                // then
                then(notificationDeliveryRepository).should().markPublished(PUBLISHABLE_IDS, NOW);
            }
        }

        @Nested
        @DisplayName("브로커가 일부 발송 기록만 받았다고 확인한 경우")
        class WhenPartiallyConfirmed {

            @Test
            @DisplayName("브로커가 확인한 발송 기록만 발행했다고 남긴다")
            void shouldMarkOnlyConfirmedPublishedWhenPartiallyConfirmed() {
                // given
                given(notificationDeliveryPublisher.publishAll(PUBLISHABLE_IDS))
                        .willReturn(List.of(FIRST_DELIVERY_ID, THIRD_DELIVERY_ID));

                // when
                publishNotificationDeliveriesService.publishPending();

                // then
                then(notificationDeliveryRepository)
                        .should()
                        .markPublished(List.of(FIRST_DELIVERY_ID, THIRD_DELIVERY_ID), NOW);
            }
        }
    }
}
