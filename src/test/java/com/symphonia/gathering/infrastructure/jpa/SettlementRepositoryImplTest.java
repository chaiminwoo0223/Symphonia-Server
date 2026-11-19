package com.symphonia.gathering.infrastructure.jpa;

import static com.symphonia.common.helper.ConcurrencyHelper.await;
import static com.symphonia.common.helper.ConcurrencyHelper.runConcurrently;
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
import com.symphonia.gathering.domain.vo.ShareStatus;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringRoundFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import com.symphonia.gathering.infrastructure.crypto.config.GatheringCryptoConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
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
    private Long roundId;
    private Long secondRoundId;
    private Long kimId;
    private Long leeId;
    private Long parkId;

    @BeforeEach
    void setUp() {
        Gathering opened =
                GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                        List.of(
                                ParticipantFixture.KIM.create(),
                                ParticipantFixture.LEE.create(),
                                ParticipantFixture.PARK.create()));
        opened.addRound(GatheringRoundFixture.SECOND.getNumber());
        Gathering gathering = saveInTransaction(opened);
        List<Long> participantIds =
                gathering.getParticipants().stream().map(Participant::getId).toList();
        gatheringId = gathering.getId();
        roundId = gathering.getRounds().getFirst().getId();
        secondRoundId = gathering.getRounds().getLast().getId();
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
                                gatheringId, roundId, kimId, List.of(kimId, leeId, parkId));

                // when
                Settlement saved = settlementRepository.save(settlement);

                // then
                assertThat(saved.getId()).isNotNull();
                assertThat(saved.getVersion()).isNotNull();
            }

            @Test
            @DisplayName("차수 id를 저장해 조회할 때 그대로 돌려준다")
            void shouldPersistRoundId() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.create(
                                gatheringId, secondRoundId, kimId, List.of(kimId, leeId, parkId));

                // when
                Settlement saved = settlementRepository.save(settlement);

                // then
                clearPersistenceContext();
                assertThat(settlementRepository.findById(saved.getId()).orElseThrow().getRoundId())
                        .isEqualTo(secondRoundId);
            }

            @Test
            @DisplayName("분담금을 인원 순서대로 저장해 같은 순서로 조회된다")
            void shouldPersistSharesInParticipantOrder() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.create(
                                gatheringId, roundId, kimId, List.of(kimId, parkId, leeId));

                // when
                Settlement saved = settlementRepository.save(settlement);

                // then
                clearPersistenceContext();
                assertThat(settlementRepository.findById(saved.getId()).orElseThrow().getShares())
                        .extracting(
                                SettlementShare::getParticipantId,
                                SettlementShare::getAmount,
                                SettlementShare::isFixed,
                                SettlementShare::getStatus)
                        .containsExactly(
                                tuple(parkId, 3_333L, false, ShareStatus.UNPAID),
                                tuple(leeId, 3_333L, false, ShareStatus.UNPAID));
            }
        }

        @Nested
        @DisplayName("이미 저장한 정산 건을 수정한 경우")
        class WhenSettlementIsUpdated {

            @Test
            @DisplayName("바꾼 차수 id를 저장해 조회할 때 돌려준다")
            void shouldPersistChangedRoundId() {
                // given
                Long settlementId = recordDinner(kimId, List.of(kimId, leeId, parkId)).getId();
                clearPersistenceContext();
                Settlement loaded = settlementRepository.findById(settlementId).orElseThrow();
                loaded.update(
                        secondRoundId,
                        SettlementFixture.DINNER.createSplitMethod(
                                kimId, List.of(kimId, leeId, parkId)),
                        LocalDateTime.now());

                // when
                settlementRepository.save(loaded);

                // then
                clearPersistenceContext();
                assertThat(settlementRepository.findById(settlementId).orElseThrow().getRoundId())
                        .isEqualTo(secondRoundId);
            }

            @Test
            @DisplayName("기존 분담금을 지우고 수정한 분담금으로 바꿔 저장한다")
            void shouldReplaceShares() {
                // given
                Long settlementId = recordDinner(kimId, List.of(kimId, leeId, parkId)).getId();
                clearPersistenceContext();
                Settlement loaded = settlementRepository.findById(settlementId).orElseThrow();
                loaded.update(
                        roundId,
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
        @DisplayName("이미 저장한 정산 건의 분담금 상태만 바꾼 경우")
        class WhenOnlyShareStatusChanged {

            @Test
            @DisplayName("정산 건 행이 그대로여도 버전을 올리고 바꾼 분담금 상태를 저장한다")
            void shouldIncrementVersion() {
                // given
                Long settlementId = recordDinner(kimId, List.of(kimId, leeId, parkId)).getId();
                clearPersistenceContext();
                Settlement loaded = settlementRepository.findById(settlementId).orElseThrow();
                loaded.markShareSent(leeId, LocalDateTime.now());

                // when
                Settlement saved = settlementRepository.save(loaded);

                // then
                clearPersistenceContext();
                Settlement reloaded = settlementRepository.findById(settlementId).orElseThrow();
                assertThat(saved.getVersion()).isEqualTo(loaded.getVersion() + 1);
                assertThat(reloaded.getVersion()).isEqualTo(loaded.getVersion() + 1);
                assertThat(reloaded.getShares())
                        .extracting(SettlementShare::getParticipantId, SettlementShare::getStatus)
                        .containsExactly(
                                tuple(leeId, ShareStatus.SENT), tuple(parkId, ShareStatus.UNPAID));
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
                        roundId,
                        SettlementFixture.SECOND_ROUND.createSplitMethod(
                                kimId, List.of(kimId, leeId)),
                        LocalDateTime.now());

                // when & then
                assertThatThrownBy(() -> settlementRepository.save(stale))
                        .isInstanceOf(SettlementConflictException.class);
            }

            @Test
            @DisplayName("먼저 저장한 요청이 분담금 상태만 바꿨어도 SettlementConflictException이 발생한다")
            void shouldThrowSettlementConflictExceptionWhenOnlyShareStatusChangedFirst() {
                // given: 두 요청이 같은 버전의 정산 건을 읽고 한 요청이 분담금 상태만 바꿔 먼저 저장한 상태
                Long settlementId = recordDinner(kimId, List.of(kimId, leeId, parkId)).getId();
                clearPersistenceContext();
                Settlement first = settlementRepository.findById(settlementId).orElseThrow();
                Settlement stale = settlementRepository.findById(settlementId).orElseThrow();
                first.markShareSent(leeId, LocalDateTime.now());
                settlementRepository.save(first);
                clearPersistenceContext();
                stale.cancel(LocalDateTime.now());

                // when & then
                assertThatThrownBy(() -> settlementRepository.save(stale))
                        .isInstanceOf(SettlementConflictException.class);
            }
        }

        // 두 트랜잭션이 실제로 동시에 커밋해야 하므로 테스트 트랜잭션 없이 실행하고, 커밋된 행은 테스트가 끝나면 지운다.
        @Nested
        @DisplayName("두 요청이 같은 정산 건을 동시에 바꿔 저장하는 경우")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        class WhenUpdatedConcurrently {

            @AfterEach
            void tearDown() {
                deleteGatheringRows();
            }

            @Test
            @DisplayName("한 요청만 저장되고 다른 요청은 SettlementConflictException이 발생한다")
            void shouldSaveOnlyOne() {
                // given
                Long settlementId =
                        transactionTemplate.execute(
                                _ -> recordDinner(kimId, List.of(kimId, leeId, parkId)).getId());
                CountDownLatch loaded = new CountDownLatch(2);

                // when
                List<Throwable> failures =
                        runConcurrently(
                                () ->
                                        saveAfterBothLoaded(
                                                settlementId,
                                                settlement ->
                                                        update(settlement, List.of(kimId, leeId)),
                                                loaded),
                                () ->
                                        saveAfterBothLoaded(
                                                settlementId,
                                                settlement ->
                                                        update(
                                                                settlement,
                                                                List.of(kimId, leeId, parkId)),
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

            @Test
            @DisplayName(
                    "한 요청이 분담금 상태만 바꾸고 다른 요청이 취소해도 한 요청만 저장되고 다른 요청은 SettlementConflictException이 발생한다")
            void shouldSaveOnlyOneWhenShareStatusChangedAndCancelled() {
                // given
                Long settlementId =
                        transactionTemplate.execute(
                                _ -> recordDinner(kimId, List.of(kimId, leeId, parkId)).getId());
                CountDownLatch loaded = new CountDownLatch(2);

                // when
                List<Throwable> failures =
                        runConcurrently(
                                () ->
                                        saveAfterBothLoaded(
                                                settlementId,
                                                settlement ->
                                                        settlement.markShareSent(
                                                                leeId, LocalDateTime.now()),
                                                loaded),
                                () ->
                                        saveAfterBothLoaded(
                                                settlementId,
                                                settlement ->
                                                        settlement.cancel(LocalDateTime.now()),
                                                loaded));

                // then
                assertThat(failures)
                        .singleElement()
                        .isInstanceOf(SettlementConflictException.class);
                Settlement reloaded = settlementRepository.findById(settlementId).orElseThrow();
                assertThat(reloaded.getVersion()).isEqualTo(1L);
                assertThat(reloaded.isCancelled()).isNotEqualTo(reloaded.hasSentShares());
            }

            // 두 요청이 모두 같은 버전을 읽은 뒤에 저장하도록 맞춘다.
            private void saveAfterBothLoaded(
                    Long settlementId, Consumer<Settlement> change, CountDownLatch loaded) {
                transactionTemplate.executeWithoutResult(
                        _ -> {
                            Settlement settlement =
                                    settlementRepository.findById(settlementId).orElseThrow();
                            change.accept(settlement);
                            loaded.countDown();
                            await(loaded);
                            settlementRepository.save(settlement);
                        });
            }

            private void update(Settlement settlement, List<Long> participantIds) {
                settlement.update(
                        roundId,
                        SettlementFixture.SECOND_ROUND.createSplitMethod(kimId, participantIds),
                        LocalDateTime.now());
            }
        }
    }

    @Nested
    @DisplayName("saveIfAbsent 메서드는")
    class SaveIfAbsent {

        @Nested
        @DisplayName("처음 보는 요청 id인 경우")
        class WhenClientRequestIdIsNew {

            @Test
            @DisplayName("정산 건과 분담금을 저장하고 요청 id를 담아 조회할 때 그대로 돌려준다")
            void shouldSaveSettlementWithSharesAndClientRequestId() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.create(
                                gatheringId, roundId, kimId, List.of(kimId, leeId, parkId));

                // when
                Settlement saved = settlementRepository.saveIfAbsent(settlement);

                // then
                clearPersistenceContext();
                Settlement reloaded = settlementRepository.findById(saved.getId()).orElseThrow();
                assertThat(reloaded.getClientRequestId())
                        .isEqualTo(settlement.getClientRequestId());
                assertThat(reloaded.getRoundId()).isEqualTo(roundId);
                assertThat(reloaded.getTotalAmount())
                        .isEqualTo(SettlementFixture.DINNER.getTotalAmount());
                assertThat(reloaded.getShares())
                        .extracting(SettlementShare::getParticipantId, SettlementShare::getAmount)
                        .containsExactly(tuple(leeId, 3_333L), tuple(parkId, 3_333L));
            }

            @Test
            @DisplayName("감사 시각 컬럼을 기록 시각으로 채운다")
            void shouldFillAuditTimestampsWithRecordedAt() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.create(
                                gatheringId, roundId, kimId, List.of(kimId, leeId, parkId));

                // when
                Settlement saved = settlementRepository.saveIfAbsent(settlement);

                // then
                assertThat(
                                jdbcTemplate.queryForObject(
                                        "SELECT created_at FROM settlement WHERE id = ?",
                                        LocalDateTime.class,
                                        saved.getId()))
                        .isEqualTo(settlement.getRecordedAt());
                assertThat(
                                jdbcTemplate.queryForObject(
                                        "SELECT updated_at FROM settlement WHERE id = ?",
                                        LocalDateTime.class,
                                        saved.getId()))
                        .isEqualTo(settlement.getRecordedAt());
            }

            @Test
            @DisplayName("저장한 정산 건을 이어서 바꿔 저장해도 버전 충돌이 나지 않는다")
            void shouldSaveChangeOfSavedSettlementWithoutConflict() {
                // given
                Settlement saved =
                        settlementRepository.saveIfAbsent(
                                SettlementFixture.DINNER.create(
                                        gatheringId,
                                        roundId,
                                        kimId,
                                        List.of(kimId, leeId, parkId)));
                saved.markShareSent(leeId, LocalDateTime.now());

                // when
                Settlement changed = settlementRepository.save(saved);

                // then
                assertThat(changed.hasSentShares()).isTrue();
            }
        }

        @Nested
        @DisplayName("같은 결제자가 같은 요청 id로 이미 저장한 경우")
        class WhenSameClientRequestIdSaved {

            @Test
            @DisplayName("새로 저장하지 않고 처음 저장한 정산 건을 반환한다")
            void shouldReturnFirstSettlementWithoutSavingAgain() {
                // given
                Settlement first =
                        settlementRepository.saveIfAbsent(
                                SettlementFixture.DINNER.create(
                                        gatheringId,
                                        roundId,
                                        kimId,
                                        List.of(kimId, leeId, parkId)));
                Settlement retry =
                        Settlement.record(
                                gatheringId,
                                secondRoundId,
                                SettlementFixture.SECOND_ROUND.createSplitMethod(
                                        kimId, List.of(kimId, leeId)),
                                first.getClientRequestId(),
                                LocalDateTime.now());

                // when
                Settlement result = settlementRepository.saveIfAbsent(retry);

                // then
                assertThat(result.getId()).isEqualTo(first.getId());
                assertThat(result.getRoundId()).isEqualTo(roundId);
                assertThat(result.getTotalAmount())
                        .isEqualTo(SettlementFixture.DINNER.getTotalAmount());
                assertThat(settlementRepository.findAllByGatheringId(gatheringId)).hasSize(1);
            }
        }

        @Nested
        @DisplayName("다른 결제자가 같은 요청 id로 저장하는 경우")
        class WhenSameClientRequestIdFromOtherPayer {

            @Test
            @DisplayName("결제자마다 요청 id를 따로 보므로 두 정산 건을 모두 저장한다")
            void shouldSaveBothSettlements() {
                // given
                Settlement first =
                        settlementRepository.saveIfAbsent(
                                SettlementFixture.DINNER.create(
                                        gatheringId,
                                        roundId,
                                        kimId,
                                        List.of(kimId, leeId, parkId)));
                Settlement otherPayer =
                        Settlement.record(
                                gatheringId,
                                roundId,
                                SettlementFixture.DINNER.createSplitMethod(
                                        leeId, List.of(leeId, kimId)),
                                first.getClientRequestId(),
                                LocalDateTime.now());

                // when
                Settlement result = settlementRepository.saveIfAbsent(otherPayer);

                // then
                assertThat(result.getId()).isNotEqualTo(first.getId());
                assertThat(settlementRepository.findAllByGatheringId(gatheringId)).hasSize(2);
            }
        }

        // 두 트랜잭션이 실제로 동시에 커밋해야 하므로 테스트 트랜잭션 없이 실행하고, 커밋된 행은 테스트가 끝나면 지운다.
        @Nested
        @DisplayName("두 요청이 같은 요청 id로 동시에 저장하는 경우")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        class WhenSavedConcurrently {

            @AfterEach
            void tearDown() {
                deleteGatheringRows();
            }

            @Test
            @DisplayName("정산 건을 하나만 두고 두 요청 모두 예외 없이 같은 정산 건을 반환받는다")
            void shouldKeepSingleSettlementAndReturnSameSettlement() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.create(
                                gatheringId, roundId, kimId, List.of(kimId, leeId, parkId));
                CountDownLatch ready = new CountDownLatch(2);
                List<Long> savedIds = new CopyOnWriteArrayList<>();

                // when
                List<Throwable> failures =
                        runConcurrently(
                                () -> savedIds.add(saveIfAbsentAfterBothReady(settlement, ready)),
                                () -> savedIds.add(saveIfAbsentAfterBothReady(settlement, ready)));

                // then
                assertThat(failures).isEmpty();
                List<Settlement> stored = settlementRepository.findAllByGatheringId(gatheringId);
                assertThat(stored).hasSize(1);
                assertThat(savedIds).hasSize(2).containsOnly(stored.getFirst().getId());
                assertThat(stored.getFirst().getShares()).hasSize(2);
            }

            // 두 요청이 같은 순간에 저장을 시작하도록 맞춘다.
            private Long saveIfAbsentAfterBothReady(Settlement settlement, CountDownLatch ready) {
                return transactionTemplate.execute(
                        _ -> {
                            ready.countDown();
                            await(ready);
                            return settlementRepository.saveIfAbsent(settlement).getId();
                        });
            }
        }
    }

    @Nested
    @DisplayName("findByPayerParticipantIdAndClientRequestId 메서드는")
    class FindByPayerParticipantIdAndClientRequestId {

        @Test
        @DisplayName("같은 결제자가 같은 요청 id로 기록한 정산 건을 분담금과 함께 찾는다")
        void shouldFindSettlementOfPayerWithShares() {
            // given
            Settlement saved =
                    settlementRepository.saveIfAbsent(
                            SettlementFixture.DINNER.create(
                                    gatheringId, roundId, kimId, List.of(kimId, leeId, parkId)));

            // when
            Optional<Settlement> result =
                    settlementRepository.findByPayerParticipantIdAndClientRequestId(
                            kimId, saved.getClientRequestId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(saved.getId());
            assertThat(result.get().getShares()).hasSize(2);
        }

        @Test
        @DisplayName("다른 결제자의 같은 요청 id는 찾지 못한다")
        void shouldReturnEmptyWhenOtherPayer() {
            // given
            Settlement saved =
                    settlementRepository.saveIfAbsent(
                            SettlementFixture.DINNER.create(
                                    gatheringId, roundId, kimId, List.of(kimId, leeId, parkId)));

            // when
            Optional<Settlement> result =
                    settlementRepository.findByPayerParticipantIdAndClientRequestId(
                            leeId, saved.getClientRequestId());

            // then
            assertThat(result).isEmpty();
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
        @DisplayName("결제자가 기록한 정산 건 중 취소하지 않은 것을 기록한 순서대로 분담금과 함께 반환한다")
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

    @Nested
    @DisplayName("findAllByShareParticipantId 메서드는")
    class FindAllByShareParticipantId {

        @Test
        @DisplayName("참여자의 분담금이 있는 정산 건 중 취소하지 않은 것을 기록한 순서대로 분담금과 함께 반환한다")
        void shouldReturnSettlementsWithSharesExcludingCancelled() {
            // given
            Settlement first = recordDinner(kimId, List.of(kimId, leeId, parkId));
            Settlement second = recordDinner(parkId, List.of(parkId, leeId));
            cancel(recordDinner(kimId, List.of(kimId, leeId)));
            recordDinner(kimId, List.of(kimId, parkId));
            recordDinner(leeId, List.of(leeId, kimId));
            clearPersistenceContext();

            // when
            List<Settlement> result = settlementRepository.findAllByShareParticipantId(leeId);

            // then
            assertThat(result)
                    .extracting(Settlement::getId)
                    .containsExactly(first.getId(), second.getId());
            assertThat(result.getFirst().getShares())
                    .extracting(SettlementShare::getParticipantId)
                    .containsExactly(leeId, parkId);
            assertThat(result.getLast().getShares())
                    .extracting(SettlementShare::getParticipantId)
                    .containsExactly(leeId);
        }

        @Test
        @DisplayName("참여자의 분담금이 있는 정산 건이 없으면 빈 목록을 반환한다")
        void shouldReturnEmptyListWhenSettlementsNotExist() {
            // given
            recordDinner(leeId, List.of(leeId, kimId));

            // when
            List<Settlement> result = settlementRepository.findAllByShareParticipantId(leeId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByShareParticipantIdAndPayerForUpdate 메서드는")
    class FindAllByShareParticipantIdAndPayerForUpdate {

        @Test
        @DisplayName("그 결제자의 정산 건 중 참여자의 분담금이 있고 취소하지 않은 것을 기록한 순서대로 분담금과 함께 반환한다")
        void shouldReturnSettlementsOfPayerWithSharesExcludingCancelled() {
            // given
            Settlement first = recordDinner(kimId, List.of(kimId, leeId, parkId));
            recordDinner(parkId, List.of(parkId, leeId));
            cancel(recordDinner(kimId, List.of(kimId, leeId)));
            recordDinner(kimId, List.of(kimId, parkId));
            Settlement second = recordDinner(kimId, List.of(kimId, leeId));
            clearPersistenceContext();

            // when
            List<Settlement> result =
                    settlementRepository.findAllByShareParticipantIdAndPayerForUpdate(leeId, kimId);

            // then
            assertThat(result)
                    .extracting(Settlement::getId)
                    .containsExactly(first.getId(), second.getId());
            assertThat(result.getFirst().getShares())
                    .extracting(SettlementShare::getParticipantId)
                    .containsExactly(leeId, parkId);
            assertThat(result.getLast().getShares())
                    .extracting(SettlementShare::getParticipantId)
                    .containsExactly(leeId);
        }

        @Test
        @DisplayName("그 결제자에게 낼 분담금이 있는 정산 건이 없으면 빈 목록을 반환한다")
        void shouldReturnEmptyListWhenSettlementsNotExist() {
            // given
            recordDinner(parkId, List.of(parkId, leeId));
            recordDinner(kimId, List.of(kimId, parkId));

            // when
            List<Settlement> result =
                    settlementRepository.findAllByShareParticipantIdAndPayerForUpdate(leeId, kimId);

            // then
            assertThat(result).isEmpty();
        }

        // 두 트랜잭션이 실제로 동시에 커밋해야 하므로 테스트 트랜잭션 없이 실행하고, 커밋된 행은 테스트가 끝나면 지운다.
        @Nested
        @DisplayName("두 참여자가 같은 정산 건에서 동시에 각자 분담금을 보냄으로 바꾸는 경우")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        class WhenSentConcurrently {

            @AfterEach
            void tearDown() {
                deleteGatheringRows();
            }

            @Test
            @DisplayName("늦은 요청이 잠금을 기다렸다가 최신 상태를 읽어 두 요청 모두 저장되고 두 분담금 모두 보냄이 된다")
            void shouldSaveBothSentShares() {
                // given
                Long settlementId =
                        transactionTemplate.execute(
                                _ -> recordDinner(kimId, List.of(kimId, leeId, parkId)).getId());
                CountDownLatch ready = new CountDownLatch(2);

                // when
                List<Throwable> failures =
                        runConcurrently(
                                () -> markSentAfterBothReady(leeId, ready),
                                () -> markSentAfterBothReady(parkId, ready));

                // then
                assertThat(failures).isEmpty();
                Settlement reloaded = settlementRepository.findById(settlementId).orElseThrow();
                assertThat(reloaded.getVersion()).isEqualTo(2L);
                assertThat(reloaded.getShares())
                        .extracting(SettlementShare::getParticipantId, SettlementShare::getStatus)
                        .containsExactly(
                                tuple(leeId, ShareStatus.SENT), tuple(parkId, ShareStatus.SENT));
            }

            // 잠금 조회는 한 요청이 커밋할 때까지 다른 요청을 기다리게 하므로, 읽은 뒤가 아니라 트랜잭션 시작만 맞춘다.
            private void markSentAfterBothReady(Long participantId, CountDownLatch ready) {
                ready.countDown();
                await(ready);
                transactionTemplate.executeWithoutResult(
                        _ ->
                                settlementRepository
                                        .findAllByShareParticipantIdAndPayerForUpdate(
                                                participantId, kimId)
                                        .forEach(
                                                settlement -> {
                                                    settlement.markShareSent(
                                                            participantId, LocalDateTime.now());
                                                    settlementRepository.save(settlement);
                                                }));
            }
        }
    }

    @Nested
    @DisplayName("existsByParticipantId 메서드는")
    class ExistsByParticipantId {

        @Test
        @DisplayName("정산 건의 결제자인 참여자면 true를 반환한다")
        void shouldReturnTrueWhenParticipantIsPayer() {
            // given
            recordDinner(kimId, List.of(kimId, leeId));

            // when & then
            assertThat(settlementRepository.existsByParticipantId(kimId)).isTrue();
        }

        @Test
        @DisplayName("정산 건의 분담금 인원인 참여자면 true를 반환한다")
        void shouldReturnTrueWhenParticipantHasShare() {
            // given
            recordDinner(kimId, List.of(kimId, leeId));

            // when & then
            assertThat(settlementRepository.existsByParticipantId(leeId)).isTrue();
        }

        @Test
        @DisplayName("취소한 정산 건의 인원인 참여자도 true를 반환한다")
        void shouldReturnTrueWhenSettlementCancelled() {
            // given
            cancel(recordDinner(kimId, List.of(kimId, leeId)));

            // when & then
            assertThat(settlementRepository.existsByParticipantId(leeId)).isTrue();
        }

        @Test
        @DisplayName("어떤 정산 건에도 없는 참여자면 false를 반환한다")
        void shouldReturnFalseWhenParticipantNotInAnySettlement() {
            // given
            recordDinner(kimId, List.of(kimId, leeId));

            // when & then
            assertThat(settlementRepository.existsByParticipantId(parkId)).isFalse();
        }
    }

    private Settlement recordDinner(Long payerParticipantId, List<Long> participantIds) {
        return settlementRepository.save(
                SettlementFixture.DINNER.create(
                        gatheringId, roundId, payerParticipantId, participantIds));
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

    // 테스트 트랜잭션 없이 커밋한 모임의 행을 외래 키 순서대로 지운다.
    private void deleteGatheringRows() {
        String participantIds = "SELECT id FROM participant WHERE gathering_id = ?";
        jdbcTemplate.update(
                "DELETE FROM settlement_share WHERE settlement_id IN (SELECT id FROM settlement WHERE gathering_id = ?)",
                gatheringId);
        jdbcTemplate.update("DELETE FROM settlement WHERE gathering_id = ?", gatheringId);
        jdbcTemplate.update("DELETE FROM gathering_round WHERE gathering_id = ?", gatheringId);
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

    // 저장소는 부른 쪽의 트랜잭션을 전제로 하므로, 테스트 트랜잭션 없이 도는 중첩 클래스의 준비도 트랜잭션 안에서 저장한다.
    private Gathering saveInTransaction(Gathering gathering) {
        return transactionTemplate.execute(_ -> gatheringRepository.save(gathering));
    }
}
