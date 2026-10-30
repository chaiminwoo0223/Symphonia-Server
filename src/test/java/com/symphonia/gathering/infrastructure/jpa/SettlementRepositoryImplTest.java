package com.symphonia.gathering.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.symphonia.RepositoryTest;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.entity.SettlementShare;
import com.symphonia.gathering.domain.exception.SettlementConflictException;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.PaymentStatus;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import com.symphonia.gathering.infrastructure.crypto.config.GatheringCryptoConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    SettlementRepositoryImpl.class,
    GatheringRepositoryImpl.class,
    GatheringDataKeyLoader.class,
    GatheringMasterKey.class,
    GatheringCryptoConfig.class
})
class SettlementRepositoryImplTest extends RepositoryTest {

    @Autowired private SettlementRepository settlementRepository;
    @Autowired private SettlementShareJpaRepository settlementShareJpaRepository;
    @Autowired private GatheringRepository gatheringRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long gatheringId;
    private Long kimId;
    private Long leeId;
    private Long parkId;

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
    }

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Nested
        @DisplayName("처음 저장하는 정산 건인 경우")
        class WhenSettlementIsNew {

            @Test
            @DisplayName("id와 버전을 채워 반환한다")
            void shouldReturnSettlementWithIdAndVersion() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.create(
                                gatheringId, kimId, List.of(kimId, leeId, parkId));

                // when
                Settlement saved = settlementRepository.save(settlement);

                // then
                assertThat(saved.getId()).isNotNull();
                assertThat(saved.getVersion()).isNotNull();
            }

            @Test
            @DisplayName("분담을 인원 순서대로 저장해 같은 순서로 조회된다")
            void shouldPersistSharesInParticipantOrder() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.create(
                                gatheringId, kimId, List.of(kimId, parkId, leeId));

                // when
                Settlement saved = settlementRepository.save(settlement);

                // then
                clearPersistenceContext();
                assertThat(settlementRepository.findById(saved.getId()).orElseThrow().getShares())
                        .extracting(
                                SettlementShare::getParticipantId,
                                SettlementShare::getAmount,
                                SettlementShare::isFixed,
                                SettlementShare::getPaymentStatus)
                        .containsExactly(
                                tuple(parkId, 3_400L, false, PaymentStatus.UNPAID),
                                tuple(leeId, 3_400L, false, PaymentStatus.UNPAID));
            }
        }

        @Nested
        @DisplayName("이미 저장한 정산 건을 수정한 경우")
        class WhenSettlementIsUpdated {

            @Test
            @DisplayName("기존 분담을 지우고 수정한 분담으로 바꿔 저장한다")
            void shouldReplaceShares() {
                // given
                Long settlementId = recordDinner(kimId, List.of(kimId, leeId, parkId)).getId();
                clearPersistenceContext();
                Settlement loaded = settlementRepository.findById(settlementId).orElseThrow();
                loaded.update(
                        SettlementFixture.SECOND_ROUND.createSplitMethod(
                                kimId, List.of(kimId, leeId)),
                        LocalDateTime.now());

                // when
                settlementRepository.save(loaded);

                // then
                clearPersistenceContext();
                Settlement reloaded = settlementRepository.findById(settlementId).orElseThrow();
                assertThat(reloaded.getTotalAmount())
                        .isEqualTo(SettlementFixture.SECOND_ROUND.getTotalAmount());
                assertThat(reloaded.getShares())
                        .extracting(SettlementShare::getParticipantId, SettlementShare::getAmount)
                        .containsExactly(tuple(leeId, 10_000L));
                assertThat(
                                settlementShareJpaRepository.findAllBySettlementIdOrderById(
                                        settlementId))
                        .hasSize(1);
            }
        }

        @Nested
        @DisplayName("읽은 뒤 다른 요청이 먼저 바꾼 정산 건인 경우")
        class WhenVersionStale {

            @Test
            @DisplayName("SettlementConflictException이 발생한다")
            void shouldThrowSettlementConflictException() {
                // given: 두 요청이 같은 버전의 정산 건을 읽고 한 요청이 먼저 저장한 상태
                Long settlementId = recordDinner(kimId, List.of(kimId, leeId, parkId)).getId();
                clearPersistenceContext();
                Settlement first = settlementRepository.findById(settlementId).orElseThrow();
                Settlement stale = settlementRepository.findById(settlementId).orElseThrow();
                first.cancel(LocalDateTime.now());
                settlementRepository.save(first);
                clearPersistenceContext();
                stale.update(
                        SettlementFixture.SECOND_ROUND.createSplitMethod(
                                kimId, List.of(kimId, leeId)),
                        LocalDateTime.now());

                // when & then
                assertThatThrownBy(() -> settlementRepository.save(stale))
                        .isInstanceOf(SettlementConflictException.class);
            }
        }

        // 두 트랜잭션이 실제로 동시에 커밋해야 하므로 테스트 트랜잭션 없이 실행하고, 커밋된 행은 테스트가 끝나면 지운다.
        @Nested
        @DisplayName("두 요청이 같은 정산 건을 동시에 수정하는 경우")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        class WhenUpdatedConcurrently {

            @AfterEach
            void tearDown() {
                deleteGatheringRows();
            }

            @Test
            @DisplayName("한 요청만 저장되고 다른 요청은 SettlementConflictException이 발생한다")
            void shouldSaveOnlyOne() throws Exception {
                // given
                Long settlementId =
                        transactionTemplate.execute(
                                status ->
                                        recordDinner(kimId, List.of(kimId, leeId, parkId)).getId());
                CountDownLatch loaded = new CountDownLatch(2);

                // when
                List<Throwable> failures =
                        runConcurrently(
                                () ->
                                        updateAfterBothLoaded(
                                                settlementId, List.of(kimId, leeId), loaded),
                                () ->
                                        updateAfterBothLoaded(
                                                settlementId,
                                                List.of(kimId, leeId, parkId),
                                                loaded));

                // then
                assertThat(failures)
                        .singleElement()
                        .isInstanceOf(SettlementConflictException.class);
                Settlement reloaded = settlementRepository.findById(settlementId).orElseThrow();
                assertThat(reloaded.getVersion()).isEqualTo(1L);
                assertThat(
                                settlementShareJpaRepository.findAllBySettlementIdOrderById(
                                        settlementId))
                        .hasSameSizeAs(reloaded.getShares());
            }

            // 두 요청이 모두 같은 버전을 읽은 뒤에 저장하도록 맞춘다.
            private void updateAfterBothLoaded(
                    Long settlementId, List<Long> participantIds, CountDownLatch loaded) {
                transactionTemplate.executeWithoutResult(
                        status -> {
                            Settlement settlement =
                                    settlementRepository.findById(settlementId).orElseThrow();
                            settlement.update(
                                    SettlementFixture.SECOND_ROUND.createSplitMethod(
                                            kimId, participantIds),
                                    LocalDateTime.now());
                            loaded.countDown();
                            await(loaded);
                            settlementRepository.save(settlement);
                        });
            }
        }
    }

    @Nested
    @DisplayName("findLatestByGatheringId 메서드는")
    class FindLatestByGatheringId {

        @Test
        @DisplayName("취소한 정산 건을 빼고 가장 나중에 만든 정산 건을 반환한다")
        void shouldReturnLatestSettlementExcludingCancelled() {
            // given
            recordDinner(kimId, List.of(kimId, leeId, parkId));
            Settlement latest = recordDinner(leeId, List.of(leeId, kimId));
            cancel(recordDinner(kimId, List.of(kimId, parkId)));
            clearPersistenceContext();

            // when
            Optional<Settlement> result = settlementRepository.findLatestByGatheringId(gatheringId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(latest.getId());
            assertThat(result.get().getParticipantIds()).containsExactly(leeId, kimId);
        }

        @Test
        @DisplayName("정산 건이 없으면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenSettlementsNotExist() {
            // when
            Optional<Settlement> result = settlementRepository.findLatestByGatheringId(gatheringId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByPayerParticipantId 메서드는")
    class FindAllByPayerParticipantId {

        @Test
        @DisplayName("결제자가 기록한 정산 건 중 취소하지 않은 것을 기록한 순서대로 분담과 함께 반환한다")
        void shouldReturnSettlementsWithSharesExcludingCancelled() {
            // given
            Settlement first = recordDinner(kimId, List.of(kimId, leeId, parkId));
            Settlement second = recordDinner(kimId, List.of(kimId, parkId));
            cancel(recordDinner(kimId, List.of(kimId, leeId)));
            recordDinner(leeId, List.of(leeId, kimId));
            clearPersistenceContext();

            // when
            List<Settlement> result = settlementRepository.findAllByPayerParticipantId(kimId);

            // then
            assertThat(result)
                    .extracting(Settlement::getId)
                    .containsExactly(first.getId(), second.getId());
            assertThat(result.getFirst().getShares())
                    .extracting(SettlementShare::getParticipantId)
                    .containsExactly(leeId, parkId);
            assertThat(result.getLast().getShares())
                    .extracting(SettlementShare::getParticipantId)
                    .containsExactly(parkId);
        }

        @Test
        @DisplayName("결제자로 기록한 정산 건이 없으면 빈 목록을 반환한다")
        void shouldReturnEmptyListWhenSettlementsNotExist() {
            // when
            List<Settlement> result = settlementRepository.findAllByPayerParticipantId(parkId);

            // then
            assertThat(result).isEmpty();
        }
    }

    private Settlement recordDinner(Long payerParticipantId, List<Long> participantIds) {
        return settlementRepository.save(
                SettlementFixture.DINNER.create(gatheringId, payerParticipantId, participantIds));
    }

    private void cancel(Settlement settlement) {
        settlement.cancel(LocalDateTime.now());
        settlementRepository.save(settlement);
    }

    // 1차 캐시를 비워 다음 조회가 DB에서 다시 읽게 한다.
    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }

    // 두 작업을 서로 다른 스레드에서 동시에 실행하고, 실패한 작업의 예외만 모아 돌려준다.
    private List<Throwable> runConcurrently(Runnable first, Runnable second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> futures = List.of(executor.submit(first), executor.submit(second));
            List<Throwable> failures = new ArrayList<>();
            for (Future<?> future : futures) {
                try {
                    future.get(10, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    failures.add(e.getCause());
                }
            }
            return failures;
        } finally {
            executor.shutdownNow();
        }
    }

    // 두 요청이 같은 시점에 저장해야 경합을 확인할 수 있으므로, 제때 모이지 않으면 테스트를 실패시킨다.
    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    // 테스트 트랜잭션 없이 커밋한 모임의 행을 외래 키 순서대로 지운다.
    private void deleteGatheringRows() {
        String participantIds = "SELECT id FROM participant WHERE gathering_id = ?";
        jdbcTemplate.update(
                "DELETE FROM settlement_share WHERE settlement_id IN (SELECT id FROM settlement WHERE gathering_id = ?)",
                gatheringId);
        jdbcTemplate.update("DELETE FROM settlement WHERE gathering_id = ?", gatheringId);
        jdbcTemplate.update(
                "DELETE FROM participant_bank_account WHERE participant_id IN ("
                        + participantIds
                        + ")",
                gatheringId);
        jdbcTemplate.update(
                "DELETE FROM participant_link_token WHERE participant_id IN ("
                        + participantIds
                        + ")",
                gatheringId);
        jdbcTemplate.update("DELETE FROM participant WHERE gathering_id = ?", gatheringId);
        jdbcTemplate.update("DELETE FROM gathering_join_token WHERE gathering_id = ?", gatheringId);
        jdbcTemplate.update("DELETE FROM gathering_data_key WHERE gathering_id = ?", gatheringId);
        jdbcTemplate.update("DELETE FROM gathering WHERE id = ?", gatheringId);
    }
}
