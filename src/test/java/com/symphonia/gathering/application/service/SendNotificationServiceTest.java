package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.command.SendNotificationCommand;
import com.symphonia.gathering.application.dto.command.SendNotificationCommand.PayerAmountCommand;
import com.symphonia.gathering.domain.entity.NotificationDelivery;
import com.symphonia.gathering.domain.exception.NotificationSendFailedException;
import com.symphonia.gathering.domain.repository.NotificationDeliveryRepository;
import com.symphonia.gathering.domain.sender.NotificationSender;
import com.symphonia.gathering.domain.vo.DeliveryStatus;
import com.symphonia.gathering.domain.vo.NotificationMessage;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.NotificationDeliveryFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.global.config.TimeConfig;
import com.symphonia.notification.domain.vo.NotificationType;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@DisplayName("SendNotificationService 단위 테스트")
class SendNotificationServiceTest extends UnitTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 15, 0);
    private static final Long DELIVERY_ID = 40L;
    private static final Long GATHERING_ID = 10L;
    private static final Long SETTLEMENT_ID = 30L;
    private static final Long LEE_ID = 21L;

    private SendNotificationService sendNotificationService;

    @Mock private NotificationDeliveryRepository notificationDeliveryRepository;
    @Mock private NotificationSender notificationSender;

    @BeforeEach
    void setUp() {
        sendNotificationService =
                new SendNotificationService(
                        notificationDeliveryRepository,
                        notificationSender,
                        Clock.fixed(
                                NOW.atZone(TimeConfig.SERVICE_ZONE).toInstant(),
                                TimeConfig.SERVICE_ZONE));
    }

    @Nested
    @DisplayName("send 메서드는")
    class Send {

        @Nested
        @DisplayName("발송 기록이 없는 경우")
        class WhenDeliveryNotFound {

            @Test
            @DisplayName("보내지 않고 아무것도 저장하지 않는다")
            void shouldNotSend() {
                // given
                given(notificationDeliveryRepository.findByIdForUpdate(DELIVERY_ID))
                        .willReturn(Optional.empty());

                // when
                sendNotificationService.send(command());

                // then
                then(notificationSender).shouldHaveNoInteractions();
                then(notificationDeliveryRepository)
                        .should(never())
                        .save(any(NotificationDelivery.class));
            }
        }

        @Nested
        @DisplayName("다른 컨슈머가 먼저 끝내 보내지 않은 상태가 아닌 경우")
        class WhenDeliveryNotPending {

            @Test
            @DisplayName("다시 보내지 않고 아무것도 저장하지 않는다")
            void shouldNotSend() {
                // given
                givenDelivery(deliveryBuilder().status(DeliveryStatus.SENT).build());

                // when
                sendNotificationService.send(command());

                // then
                then(notificationSender).shouldHaveNoInteractions();
                then(notificationDeliveryRepository)
                        .should(never())
                        .save(any(NotificationDelivery.class));
            }
        }

        @Nested
        @DisplayName("외부 발송에 성공한 경우")
        class WhenSendSucceeded {

            @Test
            @DisplayName("명령의 내용을 메시지로 보내고 보낸 시각과 함께 보냄으로 바꿔 저장한다")
            void shouldMarkSent() {
                // given
                NotificationDelivery delivery = deliveryBuilder().claimedAt(NOW).build();
                givenDelivery(delivery);

                // when
                sendNotificationService.send(command());

                // then
                ArgumentCaptor<NotificationMessage> captor =
                        ArgumentCaptor.forClass(NotificationMessage.class);
                then(notificationSender).should().send(captor.capture());
                assertThat(captor.getValue().deliveryId()).isEqualTo(DELIVERY_ID);
                assertThat(captor.getValue().recipientPhoneNumber().value())
                        .isEqualTo(ParticipantFixture.LEE.getPhoneNumber());
                assertThat(captor.getValue().getTotalAmount()).isEqualTo(3_333L);
                then(notificationDeliveryRepository).should().save(delivery);
                assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SENT);
                assertThat(delivery.getSentAt()).isEqualTo(NOW);
            }
        }

        @Nested
        @DisplayName("외부 발송에 실패한 경우")
        class WhenSendFailed {

            @BeforeEach
            void setUp() {
                willThrow(new NotificationSendFailedException())
                        .given(notificationSender)
                        .send(any(NotificationMessage.class));
            }

            @Test
            @DisplayName("첫 실패면 시도 횟수를 1로 남기고 1분 뒤에 다시 보내도록 발행 시각과 맡은 시각을 비워 저장한다")
            void shouldRetryAfterOneMinuteWhenFirstSendFailed() {
                // given
                NotificationDelivery delivery = givenDeliveryFailedBefore(0);

                // when
                sendNotificationService.send(command());

                // then
                thenRetryScheduled(delivery, 1, NOW.plusMinutes(1));
            }

            @Test
            @DisplayName("두 번째 실패면 5분 뒤에 다시 보내도록 저장한다")
            void shouldRetryAfterFiveMinutesWhenSecondSendFailed() {
                // given
                NotificationDelivery delivery = givenDeliveryFailedBefore(1);

                // when
                sendNotificationService.send(command());

                // then
                thenRetryScheduled(delivery, 2, NOW.plusMinutes(5));
            }

            @Test
            @DisplayName("세 번째 실패면 15분 뒤에 다시 보내도록 저장한다")
            void shouldRetryAfterFifteenMinutesWhenThirdSendFailed() {
                // given
                NotificationDelivery delivery = givenDeliveryFailedBefore(2);

                // when
                sendNotificationService.send(command());

                // then
                thenRetryScheduled(delivery, 3, NOW.plusMinutes(15));
            }

            @Test
            @DisplayName("네 번째 실패면 30분 뒤에 다시 보내도록 저장한다")
            void shouldRetryAfterThirtyMinutesWhenFourthSendFailed() {
                // given
                NotificationDelivery delivery = givenDeliveryFailedBefore(3);

                // when
                sendNotificationService.send(command());

                // then
                thenRetryScheduled(delivery, 4, NOW.plusMinutes(30));
            }

            @Test
            @DisplayName("다섯 번째 실패면 재시도를 다 썼으므로 실패로 끝내 저장한다")
            void shouldMarkFailedWhenFifthSendFailed() {
                // given
                NotificationDelivery delivery = givenDeliveryFailedBefore(4);

                // when
                sendNotificationService.send(command());

                // then
                then(notificationDeliveryRepository).should().save(delivery);
                assertThat(delivery.getAttempts()).isEqualTo(5);
                assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
                assertThat(delivery.getSentAt()).isNull();
            }

            private NotificationDelivery givenDeliveryFailedBefore(int attempts) {
                NotificationDelivery delivery =
                        deliveryBuilder()
                                .attempts(attempts)
                                .publishedAt(NOW.minusMinutes(1))
                                .claimedAt(NOW)
                                .build();
                givenDelivery(delivery);
                return delivery;
            }

            private void thenRetryScheduled(
                    NotificationDelivery delivery, int attempts, LocalDateTime availableAt) {
                then(notificationDeliveryRepository).should().save(delivery);
                assertThat(delivery.getAttempts()).isEqualTo(attempts);
                assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
                assertThat(delivery.getAvailableAt()).isEqualTo(availableAt);
                assertThat(delivery.getPublishedAt()).isNull();
                assertThat(delivery.getClaimedAt()).isNull();
            }
        }

        private NotificationDelivery.NotificationDeliveryBuilder deliveryBuilder() {
            return NotificationDeliveryFixture.SETTLEMENT_NOTICE.builder(
                    DELIVERY_ID, GATHERING_ID, LEE_ID, List.of(SETTLEMENT_ID));
        }

        private void givenDelivery(NotificationDelivery delivery) {
            given(notificationDeliveryRepository.findByIdForUpdate(DELIVERY_ID))
                    .willReturn(Optional.of(delivery));
        }
    }

    private SendNotificationCommand command() {
        return new SendNotificationCommand(
                DELIVERY_ID,
                NotificationType.SETTLEMENT,
                ParticipantFixture.LEE.getPhoneNumber(),
                ParticipantFixture.LEE.getName(),
                GatheringFixture.FRIDAY_DINNER.getName(),
                LocalDate.of(2026, 10, 27),
                List.of(new PayerAmountCommand(ParticipantFixture.KIM.getName(), 3_333L)),
                "link-token");
    }
}
