package com.symphonia.gathering.infrastructure.jpa;

import static com.symphonia.common.helper.ConcurrencyHelper.await;
import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.GatheringRetention;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import com.symphonia.gathering.infrastructure.crypto.config.GatheringCryptoConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    GatheringRetentionRepositoryImpl.class,
    SettlementRepositoryImpl.class,
    GatheringRepositoryImpl.class,
    GatheringDataKeyLoader.class,
    GatheringMasterKey.class,
    GatheringCryptoConfig.class
})
class GatheringRetentionRepositoryImplTest extends RepositoryTest {

    // GatheringFixture가 모임을 여는 시각이다.
    private static final LocalDateTime OPENED_AT = LocalDateTime.of(2026, 10, 27, 18, 0);
    private static final LocalDateTime MAX_DELETES_AT = OPENED_AT.plusDays(29);

    @Autowired private GatheringRetentionRepository gatheringRetentionRepository;
    @Autowired private GatheringRepository gatheringRepository;
    @Autowired private SettlementRepository settlementRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long gatheringId;
    private Long kimId;
    private Long leeId;

    @BeforeEach
    void setUp() {
        Gathering gathering =
                gatheringRepository.save(
                        GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                List.of(
                                        ParticipantFixture.KIM.create(),
                                        ParticipantFixture.LEE.create())));
        List<Long> participantIds =
                gathering.getParticipants().stream().map(Participant::getId).toList();
        gatheringId = gathering.getId();
        kimId = participantIds.get(0);
        leeId = participantIds.get(1);
    }

    @Nested
    @DisplayName("findByParticipantId 메서드는")
    class FindByParticipantId {

        @Test
        @DisplayName("취소한 정산 건까지 읽고, 분담금 상태를 바꾼 시각을 저장했다가 읽어 파기 시각을 계산한다")
        void shouldCalculateDeletesAtFromSavedStatusChangedAt() {
            // given
            LocalDateTime sentAt = OPENED_AT.plusDays(5);
            LocalDateTime cancelledAt = OPENED_AT.plusDays(6);
            Settlement sent = recordDinner();
            sent.markShareSent(leeId, sentAt);
            settlementRepository.save(sent);
            Settlement cancelled = recordDinner();
            cancelled.cancel(cancelledAt);
            settlementRepository.save(cancelled);
            clearPersistenceContext();

            // when
            GatheringRetention result =
                    gatheringRetentionRepository.findByParticipantId(leeId).orElseThrow();

            // then
            assertThat(result.gatheringId()).isEqualTo(gatheringId);
            assertThat(result.deletesAt()).isEqualTo(cancelledAt.plusDays(7));
            assertThat(result.dataKeyDiscarded()).isFalse();
        }

        @Test
        @DisplayName("미입금 분담금이 남았으면 모임을 연 지 29일 뒤를 파기 시각으로 반환한다")
        void shouldReturnMaxDeletesAtWhenUnpaidSharesRemain() {
            // given
            recordDinner();
            clearPersistenceContext();

            // when
            GatheringRetention result =
                    gatheringRetentionRepository.findByParticipantId(leeId).orElseThrow();

            // then
            assertThat(result.deletesAt()).isEqualTo(MAX_DELETES_AT);
        }

        @Test
        @DisplayName("데이터 키를 지운 모임이면 참여자를 복호화하지 않고 데이터 키를 지운 것으로 반환한다")
        void shouldReturnDataKeyDiscardedWithoutDecrypting() {
            // given
            gatheringRepository.discardDataKeys(List.of(gatheringId));

            // when
            GatheringRetention result =
                    gatheringRetentionRepository.findByParticipantId(leeId).orElseThrow();

            // then
            assertThat(result.dataKeyDiscarded()).isTrue();
            assertThat(result.isExpired(OPENED_AT)).isTrue();
        }
    }

    @Nested
    @DisplayName("findGatheringIdsOpenedUntil 메서드는")
    class FindGatheringIdsOpenedUntil {

        @Test
        @DisplayName("기준 시각에 연 모임을 찾는다")
        void shouldFindGatheringOpenedAtBoundary() {
            // when
            List<Long> result = gatheringRetentionRepository.findGatheringIdsOpenedUntil(OPENED_AT);

            // then
            assertThat(result).contains(gatheringId);
        }

        @Test
        @DisplayName("기준 시각보다 1초 뒤에 연 모임은 찾지 않는다")
        void shouldNotFindGatheringOpenedAfterBoundary() {
            // when
            List<Long> result =
                    gatheringRetentionRepository.findGatheringIdsOpenedUntil(
                            OPENED_AT.minusSeconds(1));

            // then
            assertThat(result).doesNotContain(gatheringId);
        }
    }

    @Nested
    @DisplayName("findAllByGatheringIdsForUpdate 메서드는")
    class FindAllByGatheringIdsForUpdate {

        @Test
        @DisplayName("정산 건이 없는 모임도 파기 시각을 계산해 반환한다")
        void shouldReturnRetentionOfGatheringWithoutSettlements() {
            // when
            List<GatheringRetention> result =
                    gatheringRetentionRepository.findAllByGatheringIdsForUpdate(
                            List.of(gatheringId));

            // then
            assertThat(result)
                    .singleElement()
                    .extracting(GatheringRetention::deletesAt)
                    .isEqualTo(OPENED_AT.plusHours(48).plusDays(7));
        }

        @Nested
        @DisplayName("다른 트랜잭션이 모임을 잠근 경우")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        class WhenGatheringLocked {

            private final CountDownLatch locked = new CountDownLatch(1);
            private final CountDownLatch read = new CountDownLatch(1);
            private CompletableFuture<Void> lockHolder;

            // 잠근 트랜잭션을 끝낸 뒤 테스트 트랜잭션 없이 커밋한 모임의 행을 지운다.
            @AfterEach
            void tearDown() throws Exception {
                read.countDown();
                lockHolder.get(10, TimeUnit.SECONDS);
                jdbcTemplate.update("DELETE FROM participant WHERE gathering_id = ?", gatheringId);
                jdbcTemplate.update(
                        "DELETE FROM gathering_data_key WHERE gathering_id = ?", gatheringId);
                jdbcTemplate.update("DELETE FROM gathering WHERE id = ?", gatheringId);
            }

            @Test
            @DisplayName("기다리지 않고 그 모임을 건너뛴다")
            void shouldSkipLockedGathering() {
                // given
                lockHolder = CompletableFuture.runAsync(this::holdGatheringLock);
                await(locked);

                // when
                List<GatheringRetention> result =
                        transactionTemplate.execute(
                                _ ->
                                        gatheringRetentionRepository.findAllByGatheringIdsForUpdate(
                                                List.of(gatheringId)));

                // then
                assertThat(result).isEmpty();
            }

            // 테스트가 읽기를 마칠 때까지 모임을 잠근 채로 둔다.
            private void holdGatheringLock() {
                transactionTemplate.executeWithoutResult(
                        _ -> {
                            jdbcTemplate.queryForList(
                                    "SELECT id FROM gathering WHERE id = ? FOR UPDATE",
                                    gatheringId);
                            locked.countDown();
                            await(read);
                        });
            }
        }
    }

    private Settlement recordDinner() {
        return settlementRepository.save(
                SettlementFixture.DINNER.create(gatheringId, kimId, List.of(kimId, leeId)));
    }

    // 1차 캐시를 비워 다음 조회가 DB에서 다시 읽게 한다.
    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
