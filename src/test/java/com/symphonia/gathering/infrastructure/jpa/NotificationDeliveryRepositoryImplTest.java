package com.symphonia.gathering.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import com.symphonia.gathering.infrastructure.crypto.config.GatheringCryptoConfig;
import com.symphonia.notification.domain.entity.NotificationDelivery;
import com.symphonia.notification.domain.repository.NotificationDeliveryRepository;
import com.symphonia.notification.domain.vo.DeliveryStatus;
import com.symphonia.notification.domain.vo.NotificationType;
import com.symphonia.notification.fixture.NotificationDeliveryFixture;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
    NotificationDeliveryRepositoryImpl.class,
    SettlementRepositoryImpl.class,
    GatheringRepositoryImpl.class,
    GatheringDataKeyLoader.class,
    GatheringMasterKey.class,
    GatheringCryptoConfig.class
})
class NotificationDeliveryRepositoryImplTest extends RepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 27, 21, 0);
    private static final LocalDateTime PUBLISHED_BEFORE = NOW.minusMinutes(5);
    private static final LocalDateTime CLAIMED_BEFORE = NOW.minusMinutes(5);

    @Autowired private NotificationDeliveryRepository notificationDeliveryRepository;
    @Autowired private SettlementRepository settlementRepository;
    @Autowired private GatheringRepository gatheringRepository;
    @Autowired private EntityManager entityManager;

    private Long gatheringId;
    private Long kimId;
    private Long leeId;
    private Long parkId;
    private Long dinnerId;
    private Long secondRoundId;

    @BeforeEach
    void setUp() {
        Gathering gathering =
                gatheringRepository.save(
                        GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                List.of(
                                        ParticipantFixture.KIM.create(),
                                        ParticipantFixture.LEE.create(),
                                        ParticipantFixture.PARK.create())));
        List<Long> participantIds =
                gathering.getParticipants().stream().map(Participant::getId).toList();
        gatheringId = gathering.getId();
        kimId = participantIds.get(0);
        leeId = participantIds.get(1);
        parkId = participantIds.get(2);
        dinnerId =
                settlementRepository
                        .save(SettlementFixture.DINNER.create(gatheringId, kimId, participantIds))
                        .getId();
        secondRoundId =
                settlementRepository
                        .save(
                                SettlementFixture.SECOND_ROUND.create(
                                        gatheringId, kimId, participantIds))
                        .getId();
    }

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("id를 채워 반환하고 담을 정산 건과 함께 저장한다")
        void shouldPersistDeliveryWithSettlementIds() {
            // given
            NotificationDelivery delivery =
                    NotificationDeliveryFixture.SETTLEMENT_NOTICE.create(
                            gatheringId, leeId, List.of(dinnerId, secondRoundId));

            // when
            NotificationDelivery saved = notificationDeliveryRepository.save(delivery);

            // then
            clearPersistenceContext();
            assertThat(saved.getId()).isNotNull();
            NotificationDelivery reloaded =
                    notificationDeliveryRepository.findByIdForUpdate(saved.getId()).orElseThrow();
            assertThat(reloaded.getGatheringId()).isEqualTo(gatheringId);
            assertThat(reloaded.getParticipantId()).isEqualTo(leeId);
            assertThat(reloaded.getType()).isEqualTo(NotificationType.SETTLEMENT);
            assertThat(reloaded.getSettlementIds())
                    .containsExactlyInAnyOrder(dinnerId, secondRoundId);
            assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.PENDING);
            assertThat(reloaded.getAttempts()).isZero();
            assertThat(reloaded.getAvailableAt())
                    .isEqualTo(NotificationDeliveryFixture.SETTLEMENT_NOTICE.getAvailableAt());
            assertThat(reloaded.getPublishedAt()).isNull();
            assertThat(reloaded.getClaimedAt()).isNull();
            assertThat(reloaded.getSentAt()).isNull();
        }

        @Test
        @DisplayName("상태, 시도 횟수, 발행 시각, 맡은 시각을 바꿔 저장한다")
        void shouldPersistChangedState() {
            // given
            NotificationDelivery delivery =
                    NotificationDeliveryFixture.SETTLEMENT_NOTICE
                            .builder(null, gatheringId, leeId, List.of(dinnerId))
                            .status(DeliveryStatus.SENT)
                            .attempts(2)
                            .publishedAt(NOW.minusMinutes(2))
                            .claimedAt(NOW.minusMinutes(1))
                            .sentAt(NOW)
                            .build();

            // when
            NotificationDelivery saved = notificationDeliveryRepository.save(delivery);

            // then
            clearPersistenceContext();
            NotificationDelivery reloaded =
                    notificationDeliveryRepository.findByIdForUpdate(saved.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.SENT);
            assertThat(reloaded.getAttempts()).isEqualTo(2);
            assertThat(reloaded.getPublishedAt()).isEqualTo(NOW.minusMinutes(2));
            assertThat(reloaded.getClaimedAt()).isEqualTo(NOW.minusMinutes(1));
            assertThat(reloaded.getSentAt()).isEqualTo(NOW);
        }
    }

    @Nested
    @DisplayName("saveAll 메서드는")
    class SaveAll {

        @Test
        @DisplayName("발송 기록마다 id를 채워 반환하고 각자 담을 정산 건과 함께 저장한다")
        void shouldPersistDeliveriesWithSettlementIds() {
            // given
            List<NotificationDelivery> deliveries =
                    List.of(
                            NotificationDeliveryFixture.SETTLEMENT_NOTICE.create(
                                    gatheringId, leeId, List.of(dinnerId, secondRoundId)),
                            NotificationDeliveryFixture.CANCELLATION_NOTICE.create(
                                    gatheringId, parkId, List.of(dinnerId)));

            // when
            List<NotificationDelivery> saved = notificationDeliveryRepository.saveAll(deliveries);

            // then
            clearPersistenceContext();
            assertThat(saved).extracting(NotificationDelivery::getId).doesNotContainNull();
            NotificationDelivery settlementNotice =
                    notificationDeliveryRepository
                            .findByIdForUpdate(saved.getFirst().getId())
                            .orElseThrow();
            NotificationDelivery cancellationNotice =
                    notificationDeliveryRepository
                            .findByIdForUpdate(saved.getLast().getId())
                            .orElseThrow();
            assertThat(settlementNotice.getParticipantId()).isEqualTo(leeId);
            assertThat(settlementNotice.getType()).isEqualTo(NotificationType.SETTLEMENT);
            assertThat(settlementNotice.getSettlementIds())
                    .containsExactlyInAnyOrder(dinnerId, secondRoundId);
            assertThat(cancellationNotice.getParticipantId()).isEqualTo(parkId);
            assertThat(cancellationNotice.getType()).isEqualTo(NotificationType.CANCELLATION);
            assertThat(cancellationNotice.getSettlementIds()).containsExactly(dinnerId);
        }
    }

    @Nested
    @DisplayName("findByIdForUpdate 메서드는")
    class FindByIdForUpdate {

        @Test
        @DisplayName("발송 기록을 담을 정산 건과 함께 반환한다")
        void shouldReturnDeliveryWhenDeliveryExists() {
            // given
            Long deliveryId =
                    save(
                            NotificationDeliveryFixture.SETTLEMENT_NOTICE.builder(
                                    null, gatheringId, leeId, List.of(dinnerId)));
            clearPersistenceContext();

            // when
            Optional<NotificationDelivery> result =
                    notificationDeliveryRepository.findByIdForUpdate(deliveryId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(deliveryId);
            assertThat(result.get().getSettlementIds()).containsExactly(dinnerId);
        }

        @Test
        @DisplayName("발송 기록이 없으면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenDeliveryNotExists() {
            // when
            Optional<NotificationDelivery> result =
                    notificationDeliveryRepository.findByIdForUpdate(Long.MAX_VALUE);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findPublishableForUpdate 메서드는")
    class FindPublishableForUpdate {

        @Test
        @DisplayName("보낼 시각이 된 PENDING 발송 기록 중 발행하지 않았거나 발행한 지 오래된 것만 보낼 시각 순서대로 반환한다")
        void shouldReturnPublishableDeliveriesOrderedByAvailableAt() {
            // given
            Long latest = savePending(NOW, null);
            Long earliest = savePending(NOW.minusHours(1), null);
            Long republished = savePending(NOW.minusMinutes(50), PUBLISHED_BEFORE.minusSeconds(1));
            savePending(NOW.minusMinutes(55), PUBLISHED_BEFORE);
            savePending(NOW.plusMinutes(1), null);
            saveWithStatus(DeliveryStatus.SENT);
            saveWithStatus(DeliveryStatus.SKIPPED);
            saveWithStatus(DeliveryStatus.FAILED);
            clearPersistenceContext();

            // when
            List<Long> result =
                    notificationDeliveryRepository
                            .findPublishableForUpdate(NOW, PUBLISHED_BEFORE, CLAIMED_BEFORE, 10)
                            .stream()
                            .map(NotificationDelivery::getId)
                            .toList();

            // then
            assertThat(result).containsExactly(earliest, republished, latest);
        }

        @Test
        @DisplayName("컨슈머가 맡은 지 오래되지 않은 발송 기록은 건너뛰고, 맡은 지 오래된 발송 기록은 반환한다")
        void shouldSkipDeliveriesClaimedRecently() {
            // given
            Long expired =
                    save(
                            NotificationDeliveryFixture.SETTLEMENT_NOTICE
                                    .builder(null, gatheringId, leeId, List.of(dinnerId))
                                    .availableAt(NOW.minusHours(1))
                                    .claimedAt(CLAIMED_BEFORE.minusSeconds(1)));
            save(
                    NotificationDeliveryFixture.SETTLEMENT_NOTICE
                            .builder(null, gatheringId, parkId, List.of(dinnerId))
                            .availableAt(NOW.minusHours(1))
                            .claimedAt(CLAIMED_BEFORE));
            clearPersistenceContext();

            // when
            List<Long> result =
                    notificationDeliveryRepository
                            .findPublishableForUpdate(NOW, PUBLISHED_BEFORE, CLAIMED_BEFORE, 10)
                            .stream()
                            .map(NotificationDelivery::getId)
                            .toList();

            // then
            assertThat(result).containsExactly(expired);
        }

        @Test
        @DisplayName("한 번에 찾을 개수를 넘으면 보낼 시각이 이른 발송 기록부터 그 개수만큼 반환한다")
        void shouldReturnDeliveriesUpToLimit() {
            // given
            Long second = savePending(NOW.minusMinutes(30), null);
            Long first = savePending(NOW.minusHours(1), null);
            savePending(NOW, null);
            clearPersistenceContext();

            // when
            List<Long> result =
                    notificationDeliveryRepository
                            .findPublishableForUpdate(NOW, PUBLISHED_BEFORE, CLAIMED_BEFORE, 2)
                            .stream()
                            .map(NotificationDelivery::getId)
                            .toList();

            // then
            assertThat(result).containsExactly(first, second);
        }
    }

    @Nested
    @DisplayName("markPublished 메서드는")
    class MarkPublished {

        @Test
        @DisplayName("주어진 발송 기록에만 발행 시각을 남긴다")
        void shouldSetPublishedAtOnlyForGivenIds() {
            // given
            Long first = savePending(NOW.minusHours(1), null);
            Long second = savePending(NOW.minusMinutes(30), null);
            Long untouched = savePending(NOW.minusMinutes(10), null);
            clearPersistenceContext();

            // when
            notificationDeliveryRepository.markPublished(List.of(first, second), NOW);

            // then
            clearPersistenceContext();
            assertThat(findPublishedAt(first)).isEqualTo(NOW);
            assertThat(findPublishedAt(second)).isEqualTo(NOW);
            assertThat(findPublishedAt(untouched)).isNull();
        }

        @Test
        @DisplayName("발송 기록이 비어 있으면 아무것도 바꾸지 않는다")
        void shouldChangeNothingWhenIdsEmpty() {
            // given
            Long deliveryId = savePending(NOW.minusHours(1), null);
            clearPersistenceContext();

            // when
            notificationDeliveryRepository.markPublished(List.of(), NOW);

            // then
            clearPersistenceContext();
            assertThat(findPublishedAt(deliveryId)).isNull();
        }
    }

    @Nested
    @DisplayName("findAllSettlementDeliveriesForUpdate 메서드는")
    class FindAllSettlementDeliveriesForUpdate {

        @Test
        @DisplayName("정산 건이 담긴 정산 메시지의 발송 기록을 상태와 상관없이 id 순서대로 반환하고, 취소 알림과 다른 정산 건만 담은 발송 기록은 뺀다")
        void shouldReturnSettlementDeliveriesContainingSettlementOrderedById() {
            // given
            saveSent(NotificationDeliveryFixture.SETTLEMENT_NOTICE, leeId, dinnerId, secondRoundId);
            Long pending =
                    save(
                            NotificationDeliveryFixture.SETTLEMENT_NOTICE.builder(
                                    null, gatheringId, parkId, List.of(dinnerId)));
            saveSent(NotificationDeliveryFixture.SETTLEMENT_NOTICE, kimId, secondRoundId);
            saveSent(NotificationDeliveryFixture.CANCELLATION_NOTICE, kimId, dinnerId);
            clearPersistenceContext();

            // when
            List<NotificationDelivery> result =
                    notificationDeliveryRepository.findAllSettlementDeliveriesForUpdate(dinnerId);

            // then
            assertThat(result)
                    .extracting(NotificationDelivery::getParticipantId)
                    .containsExactly(leeId, parkId);
            assertThat(result.getLast().getId()).isEqualTo(pending);
            assertThat(result.getLast().getStatus()).isEqualTo(DeliveryStatus.PENDING);
        }

        @Test
        @DisplayName("정산 건이 담긴 정산 메시지의 발송 기록이 없으면 빈 목록을 반환한다")
        void shouldReturnEmptyListWhenNoSettlementDeliveryContainsSettlement() {
            // given
            saveSent(NotificationDeliveryFixture.CANCELLATION_NOTICE, leeId, dinnerId);
            saveSent(NotificationDeliveryFixture.SETTLEMENT_NOTICE, parkId, secondRoundId);
            clearPersistenceContext();

            // when
            List<NotificationDelivery> result =
                    notificationDeliveryRepository.findAllSettlementDeliveriesForUpdate(dinnerId);

            // then
            assertThat(result).isEmpty();
        }
    }

    private Long save(NotificationDelivery.NotificationDeliveryBuilder builder) {
        return notificationDeliveryRepository.save(builder.build()).getId();
    }

    private Long savePending(LocalDateTime availableAt, LocalDateTime publishedAt) {
        return save(
                NotificationDeliveryFixture.SETTLEMENT_NOTICE
                        .builder(null, gatheringId, leeId, List.of(dinnerId))
                        .availableAt(availableAt)
                        .publishedAt(publishedAt));
    }

    // 보낼 시각이 지났고 발행하지 않은 발송 기록이라, 상태만으로 발행 대상에서 빠지는지 확인한다.
    private void saveWithStatus(DeliveryStatus status) {
        save(
                NotificationDeliveryFixture.SETTLEMENT_NOTICE
                        .builder(null, gatheringId, leeId, List.of(dinnerId))
                        .availableAt(NOW.minusHours(2))
                        .status(status));
    }

    private void saveSent(
            NotificationDeliveryFixture fixture, Long participantId, Long... settlementIds) {
        save(
                fixture.builder(null, gatheringId, participantId, List.of(settlementIds))
                        .status(DeliveryStatus.SENT)
                        .sentAt(NOW));
    }

    private LocalDateTime findPublishedAt(Long deliveryId) {
        return notificationDeliveryRepository
                .findByIdForUpdate(deliveryId)
                .orElseThrow()
                .getPublishedAt();
    }

    // 1차 캐시를 비워 다음 조회가 DB에서 다시 읽게 한다.
    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
