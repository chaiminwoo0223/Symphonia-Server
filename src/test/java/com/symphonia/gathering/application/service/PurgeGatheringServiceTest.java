package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.symphonia.UnitTest;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.GatheringRetention;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.global.config.TimeConfig;
import com.symphonia.notification.application.service.PurgeNotificationsService;
import com.symphonia.notification.domain.repository.NotificationDeliveryRepository;
import com.symphonia.notification.domain.repository.NotificationScheduleRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

@DisplayName("PurgeGatheringService 단위 테스트")
class PurgeGatheringServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long SETTLEMENT_ID = 30L;
    private static final Long SECOND_SETTLEMENT_ID = 31L;
    private static final Long PAYER_ID = 20L;
    private static final Long LEE_ID = 21L;
    private static final Long PARK_ID = 22L;
    private static final List<Long> PARTICIPANT_IDS = List.of(PAYER_ID, LEE_ID, PARK_ID);
    private static final LocalDateTime OPENED_AT = LocalDateTime.of(2026, 10, 1, 18, 0);
    private static final LocalDateTime RECORDING_CLOSES_AT = OPENED_AT.plusHours(48);
    private static final LocalDateTime MAX_DELETES_AT = OPENED_AT.plusDays(29);

    // 기록 기간이 끝난 뒤에 마지막으로 바뀐 정산 건의 경계다.
    private static final LocalDateTime COMPLETED_AT = RECORDING_CLOSES_AT.plusDays(3);

    private PurgeGatheringService purgeGatheringService;

    @Mock private GatheringRetentionRepository gatheringRetentionRepository;
    @Mock private GatheringRepository gatheringRepository;
    @Mock private GatheringJoinTokenRepository gatheringJoinTokenRepository;
    @Mock private ParticipantLinkTokenRepository participantLinkTokenRepository;
    @Mock private ParticipantBankAccountRepository participantBankAccountRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private NotificationScheduleRepository notificationScheduleRepository;
    @Mock private NotificationDeliveryRepository notificationDeliveryRepository;

    @Nested
    @DisplayName("discardDataKeyIfExpired 메서드는")
    class DiscardDataKeyIfExpired {

        @Nested
        @DisplayName("미입금 분담금이 남은 경우")
        class WhenUnpaidSharesRemain {

            @Test
            @DisplayName("모임을 연 지 29일이 되기 1초 전이면 데이터 키를 지우지 않는다")
            void shouldKeepDataKeyBeforeMaxRetention() {
                // given
                givenNow(MAX_DELETES_AT.minusSeconds(1));
                givenRetention(unpaidSettlement());

                // when
                boolean result = purgeGatheringService.discardDataKeyIfExpired(GATHERING_ID);

                // then
                assertThat(result).isFalse();
                then(gatheringRepository).should(never()).discardDataKey(anyLong());
            }

            @Test
            @DisplayName("모임을 연 지 29일이 되는 순간 정산이 끝나지 않았어도 데이터 키를 지운다")
            void shouldDiscardDataKeyAtMaxRetention() {
                // given
                givenNow(MAX_DELETES_AT);
                givenRetention(unpaidSettlement());

                // when
                boolean result = purgeGatheringService.discardDataKeyIfExpired(GATHERING_ID);

                // then
                assertThat(result).isTrue();
                then(gatheringRepository).should().discardDataKey(GATHERING_ID);
            }

            @Test
            @DisplayName("보냄으로 바꿨다가 미입금으로 되돌린 분담금이 있으면 끝나지 않은 것으로 보고 데이터 키를 지우지 않는다")
            void shouldKeepDataKeyWhenShareRevertedToUnpaid() {
                // given
                givenNow(COMPLETED_AT.plusDays(7));
                Settlement settlement = settledSettlementAt(COMPLETED_AT.minusDays(1));
                settlement.revertShareToUnpaid(LEE_ID, COMPLETED_AT);
                givenRetention(settlement);

                // when
                boolean result = purgeGatheringService.discardDataKeyIfExpired(GATHERING_ID);

                // then
                assertThat(result).isFalse();
            }
        }

        @Nested
        @DisplayName("기록 기간이 끝난 뒤 모든 분담금이 끝난 경우")
        class WhenSettledAfterRecordingPeriod {

            @Test
            @DisplayName("마지막 분담금이 끝나고 7일이 되기 1초 전이면 데이터 키를 지우지 않는다")
            void shouldKeepDataKeyBeforeRetentionAfterCompletion() {
                // given
                givenNow(COMPLETED_AT.plusDays(7).minusSeconds(1));
                givenRetention(settledSettlementAt(COMPLETED_AT));

                // when
                boolean result = purgeGatheringService.discardDataKeyIfExpired(GATHERING_ID);

                // then
                assertThat(result).isFalse();
            }

            @Test
            @DisplayName("마지막 분담금이 끝나고 7일이 되는 순간 데이터 키를 지운다")
            void shouldDiscardDataKeyAtRetentionAfterCompletion() {
                // given
                givenNow(COMPLETED_AT.plusDays(7));
                Settlement lastSettled =
                        SettlementFixture.SECOND_ROUND.createWithId(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID));
                lastSettled.confirmShare(LEE_ID, COMPLETED_AT);
                givenRetention(settledSettlementAt(COMPLETED_AT.minusDays(1)), lastSettled);

                // when
                boolean result = purgeGatheringService.discardDataKeyIfExpired(GATHERING_ID);

                // then
                assertThat(result).isTrue();
            }

            @Test
            @DisplayName("미입금이 남은 마지막 정산 건을 취소하면 취소한 시각부터 7일 뒤에 데이터 키를 지운다")
            void shouldDiscardDataKeyAfterLastUnpaidSettlementCancelled() {
                // given
                givenNow(COMPLETED_AT.plusDays(7));
                Settlement cancelled = unpaidSettlement();
                cancelled.cancel(COMPLETED_AT);
                givenRetention(settledSettlementAt(COMPLETED_AT.minusDays(1)), cancelled);

                // when
                boolean result = purgeGatheringService.discardDataKeyIfExpired(GATHERING_ID);

                // then
                assertThat(result).isTrue();
            }
        }

        @Nested
        @DisplayName("기록 기간이 끝나기 전에 정산이 끝난 경우")
        class WhenSettledWithinRecordingPeriod {

            @Test
            @DisplayName("기록 기간이 끝나고 7일이 되기 1초 전이면 정산 건이 더 들어올 수 있던 시각을 기준으로 보고 데이터 키를 지우지 않는다")
            void shouldKeepDataKeyBeforeRetentionAfterRecordingPeriod() {
                // given
                givenNow(RECORDING_CLOSES_AT.plusDays(7).minusSeconds(1));
                givenRetention(settledSettlementAt(OPENED_AT.plusHours(1)));

                // when
                boolean result = purgeGatheringService.discardDataKeyIfExpired(GATHERING_ID);

                // then
                assertThat(result).isFalse();
            }

            @Test
            @DisplayName("기록 기간이 끝나고 7일이 되는 순간 데이터 키를 지운다")
            void shouldDiscardDataKeyAtRetentionAfterRecordingPeriod() {
                // given
                givenNow(RECORDING_CLOSES_AT.plusDays(7));
                givenRetention(settledSettlementAt(OPENED_AT.plusHours(1)));

                // when
                boolean result = purgeGatheringService.discardDataKeyIfExpired(GATHERING_ID);

                // then
                assertThat(result).isTrue();
            }

            @Test
            @DisplayName("0원 분담금만 있는 정산 건은 기록할 때부터 끝난 것으로 보고 기록 기간이 끝나고 7일 뒤에 데이터 키를 지운다")
            void shouldDiscardDataKeyWhenOnlyZeroSharesRecorded() {
                // given
                givenNow(RECORDING_CLOSES_AT.plusDays(7));
                givenRetention(
                        SettlementFixture.DINNER.createWithFixedAmounts(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                Map.of(LEE_ID, 0L, PARK_ID, 0L)));

                // when
                boolean result = purgeGatheringService.discardDataKeyIfExpired(GATHERING_ID);

                // then
                assertThat(result).isTrue();
            }
        }

        @Nested
        @DisplayName("정산 건이 없는 경우")
        class WhenNoSettlements {

            @Test
            @DisplayName("기록 기간이 끝나고 7일이 되기 1초 전이면 데이터 키를 지우지 않는다")
            void shouldKeepDataKeyBeforeRetentionAfterRecordingPeriod() {
                // given
                givenNow(RECORDING_CLOSES_AT.plusDays(7).minusSeconds(1));
                givenRetention();

                // when
                boolean result = purgeGatheringService.discardDataKeyIfExpired(GATHERING_ID);

                // then
                assertThat(result).isFalse();
            }

            @Test
            @DisplayName("기록 기간이 끝나고 7일이 되는 순간 데이터 키를 지운다")
            void shouldDiscardDataKeyAtRetentionAfterRecordingPeriod() {
                // given
                givenNow(RECORDING_CLOSES_AT.plusDays(7));
                givenRetention();

                // when
                boolean result = purgeGatheringService.discardDataKeyIfExpired(GATHERING_ID);

                // then
                assertThat(result).isTrue();
            }
        }

        @Nested
        @DisplayName("데이터 키를 이미 지운 모임인 경우")
        class WhenDataKeyDiscarded {

            @Test
            @DisplayName("물리 삭제가 실패해 남은 모임이면 그사이 미입금으로 바뀌었어도 파기할 모임으로 본다")
            void shouldTreatAsExpiredEvenIfUnpaidAgain() {
                // given
                givenNow(OPENED_AT.plusDays(10));
                given(gatheringRetentionRepository.findByGatheringIdForUpdate(GATHERING_ID))
                        .willReturn(
                                Optional.of(
                                        GatheringRetention.of(
                                                GATHERING_ID,
                                                OPENED_AT,
                                                List.of(unpaidSettlement()),
                                                true)));

                // when
                boolean result = purgeGatheringService.discardDataKeyIfExpired(GATHERING_ID);

                // then
                assertThat(result).isTrue();
            }
        }

        @Nested
        @DisplayName("다른 서버가 파기 중인 모임인 경우")
        class WhenGatheringLockedByOtherServer {

            @Test
            @DisplayName("잠그지 못한 모임은 기다리지 않고 건너뛰어 데이터 키를 지우지 않는다")
            void shouldSkipGatheringNotLocked() {
                // given
                givenNow(MAX_DELETES_AT);
                given(gatheringRetentionRepository.findByGatheringIdForUpdate(GATHERING_ID))
                        .willReturn(Optional.empty());

                // when
                boolean result = purgeGatheringService.discardDataKeyIfExpired(GATHERING_ID);

                // then
                assertThat(result).isFalse();
                then(gatheringRepository).should(never()).discardDataKey(anyLong());
            }
        }
    }

    @Nested
    @DisplayName("purge 메서드는")
    class Purge {

        @Test
        @DisplayName("발송 예약과 발송 기록부터 참조하는 쪽 순서로 모임의 행을 테이블마다 한 번씩 지운다")
        void shouldDeleteRowsInForeignKeyOrder() {
            // given
            givenNow(MAX_DELETES_AT);

            // when
            purgeGatheringService.purge(GATHERING_ID);

            // then
            InOrder inOrder =
                    inOrder(
                            notificationScheduleRepository,
                            notificationDeliveryRepository,
                            participantLinkTokenRepository,
                            participantBankAccountRepository,
                            settlementRepository,
                            gatheringJoinTokenRepository,
                            gatheringRepository);
            inOrder.verify(notificationScheduleRepository).deleteAllByGatheringId(GATHERING_ID);
            inOrder.verify(notificationDeliveryRepository).deleteAllByGatheringId(GATHERING_ID);
            inOrder.verify(participantLinkTokenRepository).deleteAllByGatheringId(GATHERING_ID);
            inOrder.verify(participantBankAccountRepository).deleteAllByGatheringId(GATHERING_ID);
            inOrder.verify(settlementRepository).deleteAllByGatheringId(GATHERING_ID);
            inOrder.verify(gatheringJoinTokenRepository).deleteAllByGatheringId(GATHERING_ID);
            inOrder.verify(gatheringRepository).delete(GATHERING_ID);
        }
    }

    // 결제자 말고 이와 박의 분담금이 모두 미입금인 정산 건이다.
    private Settlement unpaidSettlement() {
        return SettlementFixture.DINNER.createWithId(
                SETTLEMENT_ID, GATHERING_ID, PAYER_ID, PARTICIPANT_IDS);
    }

    // 박은 하루 먼저 보냈고, 이가 settledAt에 보내 모든 분담금이 끝난 정산 건이다.
    private Settlement settledSettlementAt(LocalDateTime settledAt) {
        Settlement settlement = unpaidSettlement();
        settlement.markShareSent(PARK_ID, settledAt.minusDays(1));
        settlement.markShareSent(LEE_ID, settledAt);

        return settlement;
    }

    private void givenRetention(Settlement... settlements) {
        given(gatheringRetentionRepository.findByGatheringIdForUpdate(GATHERING_ID))
                .willReturn(
                        Optional.of(
                                GatheringRetention.of(
                                        GATHERING_ID, OPENED_AT, List.of(settlements), false)));
    }

    private void givenNow(LocalDateTime now) {
        Clock clock =
                Clock.fixed(
                        now.atZone(TimeConfig.SERVICE_ZONE).toInstant(), TimeConfig.SERVICE_ZONE);
        purgeGatheringService =
                new PurgeGatheringService(
                        gatheringRetentionRepository,
                        gatheringRepository,
                        gatheringJoinTokenRepository,
                        participantLinkTokenRepository,
                        participantBankAccountRepository,
                        settlementRepository,
                        new PurgeNotificationsService(
                                notificationScheduleRepository, notificationDeliveryRepository),
                        clock);
    }
}
