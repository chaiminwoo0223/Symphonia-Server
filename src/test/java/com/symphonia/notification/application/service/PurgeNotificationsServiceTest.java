package com.symphonia.notification.application.service;

import static org.mockito.Mockito.inOrder;

import com.symphonia.UnitTest;
import com.symphonia.notification.domain.repository.NotificationDeliveryRepository;
import com.symphonia.notification.domain.repository.NotificationScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("PurgeNotificationsService 단위 테스트")
class PurgeNotificationsServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;

    @InjectMocks private PurgeNotificationsService purgeNotificationsService;

    @Mock private NotificationScheduleRepository notificationScheduleRepository;
    @Mock private NotificationDeliveryRepository notificationDeliveryRepository;

    @Nested
    @DisplayName("purge 메서드는")
    class Purge {

        @Test
        @DisplayName("정산 건을 바꾸는 흐름과 같은 순서가 되게 발송 예약을 먼저 지우고 발송 기록을 지운다")
        void shouldDeleteScheduleBeforeDeliveries() {
            // when
            purgeNotificationsService.purge(GATHERING_ID);

            // then
            InOrder inOrder =
                    inOrder(notificationScheduleRepository, notificationDeliveryRepository);
            inOrder.verify(notificationScheduleRepository).deleteAllByGatheringId(GATHERING_ID);
            inOrder.verify(notificationDeliveryRepository).deleteAllByGatheringId(GATHERING_ID);
        }
    }
}
