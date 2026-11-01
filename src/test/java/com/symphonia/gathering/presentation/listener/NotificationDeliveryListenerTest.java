package com.symphonia.gathering.presentation.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.symphonia.IntegrationTest;
import com.symphonia.gathering.application.dto.result.PreparedNotificationResult;
import com.symphonia.gathering.application.dto.result.PreparedNotificationResult.PayerAmountResult;
import com.symphonia.gathering.application.usecase.PrepareNotificationUseCase;
import com.symphonia.gathering.application.usecase.SendNotificationUseCase;
import com.symphonia.gathering.domain.publisher.NotificationDeliveryPublisher;
import com.symphonia.gathering.domain.vo.NotificationType;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.infrastructure.messaging.config.properties.NotificationQueueProperties;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NotificationDeliveryListenerTest extends IntegrationTest {

    // 리스너는 다른 스레드에서 메시지를 받으므로 호출을 기다리는 시간이다.
    private static final long RECEIVE_TIMEOUT_MILLIS = 5_000;
    // 재시도 간격(1초, 2초)을 다 쓰고 DLQ로 옮길 때까지 기다리는 시간이다.
    private static final long DEAD_LETTER_TIMEOUT_MILLIS = 15_000;
    // send를 부르지 않는지 확인하려고 prepare 뒤에 더 기다리는 시간이다.
    private static final long NO_SEND_WAIT_MILLIS = 500;

    @Autowired private NotificationDeliveryPublisher notificationDeliveryPublisher;
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private AmqpAdmin amqpAdmin;
    @Autowired private NotificationQueueProperties notificationQueueProperties;

    @MockitoBean private PrepareNotificationUseCase prepareNotificationUseCase;
    @MockitoBean private SendNotificationUseCase sendNotificationUseCase;

    @BeforeEach
    void setUp() {
        amqpAdmin.purgeQueue(notificationQueueProperties.deadLetter(), false);
    }

    @Nested
    @DisplayName("receive 메서드는")
    class Receive {

        @Nested
        @DisplayName("보낼 메시지를 준비한 경우")
        class WhenPrepared {

            @Test
            @DisplayName("받은 발송 기록 id로 메시지를 준비하고 같은 발송 기록 id를 담아 보낸다")
            void shouldSendPreparedNotification() {
                // given
                Long deliveryId = 1L;
                given(prepareNotificationUseCase.prepare(deliveryId))
                        .willReturn(Optional.of(preparedNotification(deliveryId)));

                // when
                notificationDeliveryPublisher.publishAll(List.of(deliveryId));

                // then
                verify(prepareNotificationUseCase, timeout(RECEIVE_TIMEOUT_MILLIS))
                        .prepare(deliveryId);
                verify(sendNotificationUseCase, timeout(RECEIVE_TIMEOUT_MILLIS))
                        .send(
                                argThat(
                                        command ->
                                                deliveryId.equals(command.notificationDeliveryId())
                                                        && command.type()
                                                                == NotificationType.SETTLEMENT));
            }
        }

        @Nested
        @DisplayName("보낼 메시지가 없는 경우")
        class WhenNothingToSend {

            @Test
            @DisplayName("메시지를 보내지 않는다")
            void shouldNotSendNotification() {
                // given
                Long deliveryId = 2L;
                given(prepareNotificationUseCase.prepare(deliveryId)).willReturn(Optional.empty());

                // when
                notificationDeliveryPublisher.publishAll(List.of(deliveryId));

                // then
                verify(prepareNotificationUseCase, timeout(RECEIVE_TIMEOUT_MILLIS))
                        .prepare(deliveryId);
                verify(sendNotificationUseCase, after(NO_SEND_WAIT_MILLIS).never()).send(any());
            }
        }

        @Nested
        @DisplayName("메시지 준비가 계속 실패하는 경우")
        class WhenPrepareFails {

            @Test
            @DisplayName("세 번 시도한 뒤 메시지를 DLQ로 옮기고 보내지 않는다")
            void shouldMoveMessageToDeadLetterQueueAfterRetries() {
                // given
                Long deliveryId = 3L;
                given(prepareNotificationUseCase.prepare(deliveryId))
                        .willThrow(new IllegalStateException("prepare failed"));

                // when
                notificationDeliveryPublisher.publishAll(List.of(deliveryId));

                // then
                Object deadLetter =
                        rabbitTemplate.receiveAndConvert(
                                notificationQueueProperties.deadLetter(),
                                DEAD_LETTER_TIMEOUT_MILLIS);
                assertThat(deadLetter).isEqualTo(deliveryId.toString());
                verify(prepareNotificationUseCase, times(3)).prepare(deliveryId);
                verify(sendNotificationUseCase, never()).send(any());
            }
        }
    }

    private PreparedNotificationResult preparedNotification(Long deliveryId) {
        return new PreparedNotificationResult(
                deliveryId,
                NotificationType.SETTLEMENT,
                ParticipantFixture.LEE.getPhoneNumber(),
                ParticipantFixture.LEE.getName(),
                GatheringFixture.FRIDAY_DINNER.getName(),
                LocalDate.of(2026, 10, 27),
                List.of(new PayerAmountResult(ParticipantFixture.KIM.getName(), 3_333L)),
                "link-token");
    }
}
