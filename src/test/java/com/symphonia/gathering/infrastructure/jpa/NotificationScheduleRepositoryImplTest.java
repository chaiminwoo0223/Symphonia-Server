package com.symphonia.gathering.infrastructure.jpa;

import static com.symphonia.gathering.helper.ConcurrencyHelper.await;
import static com.symphonia.gathering.helper.ConcurrencyHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import com.symphonia.gathering.infrastructure.crypto.config.GatheringCryptoConfig;
import com.symphonia.notification.domain.entity.NotificationSchedule;
import com.symphonia.notification.domain.repository.NotificationScheduleRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Import({
    NotificationScheduleRepositoryImpl.class,
    GatheringRepositoryImpl.class,
    GatheringDataKeyLoader.class,
    GatheringMasterKey.class,
    GatheringCryptoConfig.class
})
class NotificationScheduleRepositoryImplTest extends RepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 27, 21, 0);

    @Autowired private NotificationScheduleRepository notificationScheduleRepository;
    @Autowired private GatheringRepository gatheringRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("findOrCreateForUpdate 메서드는")
    class FindOrCreateForUpdate {

        @Nested
        @DisplayName("모임의 예약이 없는 경우")
        class WhenScheduleNotExists {

            @Test
            @DisplayName("예약 시각이 빈 예약을 만들어 반환한다")
            void shouldCreateEmptySchedule() {
                // given
                Long gatheringId = saveGathering();

                // when
                NotificationSchedule result =
                        notificationScheduleRepository.findOrCreateForUpdate(gatheringId);

                // then
                clearPersistenceContext();
                assertThat(result.getGatheringId()).isEqualTo(gatheringId);
                assertThat(result.getScheduledAt()).isNull();
                assertThat(notificationScheduleRepository.findByGatheringId(gatheringId))
                        .isPresent();
            }
        }

        @Nested
        @DisplayName("모임의 예약이 이미 있는 경우")
        class WhenScheduleExists {

            @Test
            @DisplayName("새로 만들지 않고 기존 예약을 예약 시각과 함께 반환한다")
            void shouldReturnExistingSchedule() {
                // given
                Long gatheringId = saveGathering();
                saveSchedule(gatheringId, NOW);
                clearPersistenceContext();

                // when
                NotificationSchedule result =
                        notificationScheduleRepository.findOrCreateForUpdate(gatheringId);

                // then
                assertThat(result.getGatheringId()).isEqualTo(gatheringId);
                assertThat(result.getScheduledAt()).isEqualTo(NOW);
                assertThat(countSchedules(gatheringId)).isEqualTo(1);
            }
        }
    }

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("바꾼 예약 시각을 저장한다")
        void shouldPersistScheduledAt() {
            // given
            Long gatheringId = saveGathering();
            NotificationSchedule schedule =
                    notificationScheduleRepository.findOrCreateForUpdate(gatheringId);
            schedule.reschedule(NOW);

            // when
            notificationScheduleRepository.save(schedule);

            // then
            clearPersistenceContext();
            assertThat(
                            notificationScheduleRepository
                                    .findByGatheringId(gatheringId)
                                    .orElseThrow()
                                    .getScheduledAt())
                    .isEqualTo(schedule.getScheduledAt());
        }
    }

    @Nested
    @DisplayName("findByGatheringId 메서드는")
    class FindByGatheringId {

        @Test
        @DisplayName("모임의 예약을 예약 시각과 함께 반환한다")
        void shouldReturnScheduleWhenScheduleExists() {
            // given
            Long gatheringId = saveGathering();
            saveSchedule(gatheringId, NOW);
            clearPersistenceContext();

            // when
            Optional<NotificationSchedule> result =
                    notificationScheduleRepository.findByGatheringId(gatheringId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getScheduledAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("모임의 예약이 없으면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenScheduleNotExists() {
            // given
            Long gatheringId = saveGathering();

            // when
            Optional<NotificationSchedule> result =
                    notificationScheduleRepository.findByGatheringId(gatheringId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findDueGatheringIds 메서드는")
    class FindDueGatheringIds {

        @Test
        @DisplayName("예약 시각이 지났거나 지금인 모임만 예약 시각 순서대로 반환하고, 예약 시각이 비었거나 나중인 모임은 뺀다")
        void shouldReturnDueGatheringIdsOrderedByScheduledAt() {
            // given
            Long later = saveScheduledGathering(NOW.minusMinutes(30));
            Long earliest = saveScheduledGathering(NOW.minusHours(1));
            Long onTime = saveScheduledGathering(NOW);
            saveScheduledGathering(NOW.plusMinutes(1));
            saveScheduledGathering(null);
            clearPersistenceContext();

            // when
            List<Long> result = notificationScheduleRepository.findDueGatheringIds(NOW, 10);

            // then
            assertThat(result).containsExactly(earliest, later, onTime);
        }

        @Test
        @DisplayName("한 번에 찾을 개수를 넘으면 예약 시각이 이른 모임부터 그 개수만큼 반환한다")
        void shouldReturnGatheringIdsUpToLimit() {
            // given
            Long second = saveScheduledGathering(NOW.minusMinutes(30));
            Long first = saveScheduledGathering(NOW.minusHours(1));
            saveScheduledGathering(NOW);
            clearPersistenceContext();

            // when
            List<Long> result = notificationScheduleRepository.findDueGatheringIds(NOW, 2);

            // then
            assertThat(result).containsExactly(first, second);
        }
    }

    @Nested
    @DisplayName("findDueForUpdate 메서드는")
    class FindDueForUpdate {

        @Test
        @DisplayName("예약 시각이 지난 예약을 반환한다")
        void shouldReturnScheduleWhenScheduleIsDue() {
            // given
            Long gatheringId = saveScheduledGathering(NOW.minusMinutes(1));
            clearPersistenceContext();

            // when
            Optional<NotificationSchedule> result =
                    notificationScheduleRepository.findDueForUpdate(gatheringId, NOW);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getScheduledAt()).isEqualTo(NOW.minusMinutes(1));
        }

        @Test
        @DisplayName("목록을 읽은 뒤 예약 시각이 늦춰졌으면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenScheduledLater() {
            // given
            Long gatheringId = saveScheduledGathering(NOW.plusMinutes(30));
            clearPersistenceContext();

            // when
            Optional<NotificationSchedule> result =
                    notificationScheduleRepository.findDueForUpdate(gatheringId, NOW);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("예약 시각이 비었으면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenScheduleCleared() {
            // given
            Long gatheringId = saveScheduledGathering(null);
            clearPersistenceContext();

            // when
            Optional<NotificationSchedule> result =
                    notificationScheduleRepository.findDueForUpdate(gatheringId, NOW);

            // then
            assertThat(result).isEmpty();
        }

        // 두 트랜잭션이 실제로 동시에 잠가야 하므로 테스트 트랜잭션 없이 실행하고, 커밋된 행은 테스트가 끝나면 지운다.
        @Nested
        @DisplayName("다른 트랜잭션이 예약을 잠근 경우")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        class WhenLockedByOtherTransaction {

            private Long gatheringId;

            @AfterEach
            void tearDown() {
                jdbcTemplate.update(
                        "DELETE FROM notification_schedule WHERE gathering_id = ?", gatheringId);
                jdbcTemplate.update(
                        "DELETE FROM gathering_data_key WHERE gathering_id = ?", gatheringId);
                jdbcTemplate.update("DELETE FROM gathering WHERE id = ?", gatheringId);
            }

            @Test
            @DisplayName("잠금을 기다리지 않고 빈 Optional을 반환한다")
            void shouldSkipLockedSchedule() {
                // given
                gatheringId =
                        transactionTemplate.execute(
                                _ -> saveScheduledGathering(NOW.minusMinutes(1)));
                CountDownLatch locked = new CountDownLatch(1);
                CountDownLatch released = new CountDownLatch(1);
                AtomicReference<Optional<NotificationSchedule>> skipped = new AtomicReference<>();

                // when
                List<Throwable> failures =
                        runConcurrently(
                                () -> holdLock(locked, released),
                                () -> findAfterLocked(skipped, locked, released));

                // then
                assertThat(failures).isEmpty();
                assertThat(skipped.get()).isEmpty();
            }

            // 다른 트랜잭션이 조회를 마칠 때까지 잠금을 놓지 않는다.
            private void holdLock(CountDownLatch locked, CountDownLatch released) {
                transactionTemplate.executeWithoutResult(
                        _ -> {
                            assertThat(
                                            notificationScheduleRepository.findDueForUpdate(
                                                    gatheringId, NOW))
                                    .isPresent();
                            locked.countDown();
                            await(released);
                        });
            }

            private void findAfterLocked(
                    AtomicReference<Optional<NotificationSchedule>> skipped,
                    CountDownLatch locked,
                    CountDownLatch released) {
                await(locked);
                try {
                    skipped.set(
                            transactionTemplate.execute(
                                    _ ->
                                            notificationScheduleRepository.findDueForUpdate(
                                                    gatheringId, NOW)));
                } finally {
                    released.countDown();
                }
            }
        }
    }

    private Long saveGathering() {
        return gatheringRepository.save(GatheringFixture.FRIDAY_DINNER.create()).getId();
    }

    private Long saveScheduledGathering(LocalDateTime scheduledAt) {
        Long gatheringId = saveGathering();
        saveSchedule(gatheringId, scheduledAt);

        return gatheringId;
    }

    private void saveSchedule(Long gatheringId, LocalDateTime scheduledAt) {
        notificationScheduleRepository.save(
                NotificationSchedule.builder()
                        .gatheringId(gatheringId)
                        .scheduledAt(scheduledAt)
                        .build());
    }

    private Integer countSchedules(Long gatheringId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_schedule WHERE gathering_id = ?",
                Integer.class,
                gatheringId);
    }

    // 1차 캐시를 비워 다음 조회가 DB에서 다시 읽게 한다.
    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
