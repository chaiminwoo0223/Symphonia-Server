package com.symphonia.pairing.infrastructure.jpa;

import static com.symphonia.common.helper.ConcurrencyHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.symphonia.RepositoryTest;
import com.symphonia.pairing.domain.repository.PairingSelectionSummaryRepository;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingSituation;
import com.symphonia.pairing.domain.vo.RelationshipType;
import java.util.List;
import java.util.stream.IntStream;
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

@Import(PairingSelectionSummaryRepositoryImpl.class)
class PairingSelectionSummaryRepositoryImplTest extends RepositoryTest {

    // 이 집계는 모임 페어링이 가리킨 대상이 카탈로그에서 사라져도 쌓여야 해서 FK가 없다. 아무 id나 쓴다.
    private static final Long DRINK_ID = 1_000_001L;
    private static final Long ANJU_ID = 1_000_002L;
    private static final PairingSituation LOVER_ROMANTIC =
            PairingSituation.of(RelationshipType.LOVER, MoodType.ROMANTIC);
    private static final PairingSituation BOSS_WITHOUT_MOOD =
            PairingSituation.of(RelationshipType.BOSS, null);

    @Autowired private PairingSelectionSummaryRepository pairingSelectionSummaryRepository;

    @Autowired private PairingSelectionSummaryJpaRepository pairingSelectionSummaryJpaRepository;

    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("accumulate 메서드는")
    class Accumulate {

        @Nested
        @DisplayName("상황과 조합의 집계가 없는 경우")
        class WhenSummaryNotExists {

            @Test
            @DisplayName("선택 횟수 1로 집계를 만든다")
            void shouldCreateSummaryWithOneSelection() {
                // when
                pairingSelectionSummaryRepository.accumulate(DRINK_ID, ANJU_ID, LOVER_ROMANTIC);

                // then
                assertThat(pairingSelectionSummaryJpaRepository.findAll())
                        .extracting(
                                PairingSelectionSummaryJpaEntity::getRelationshipType,
                                PairingSelectionSummaryJpaEntity::getMoodType,
                                PairingSelectionSummaryJpaEntity::getDrinkId,
                                PairingSelectionSummaryJpaEntity::getAnjuId,
                                PairingSelectionSummaryJpaEntity::getSelectionCount)
                        .containsExactly(
                                tuple(
                                        RelationshipType.LOVER,
                                        MoodType.ROMANTIC,
                                        DRINK_ID,
                                        ANJU_ID,
                                        1));
            }
        }

        @Nested
        @DisplayName("상황과 조합의 집계가 있는 경우")
        class WhenSummaryExists {

            @Test
            @DisplayName("선택 횟수에 1을 더하고 행을 새로 만들지 않는다")
            void shouldAddOneToSelectionCount() {
                // given
                pairingSelectionSummaryRepository.accumulate(DRINK_ID, ANJU_ID, LOVER_ROMANTIC);

                // when
                pairingSelectionSummaryRepository.accumulate(DRINK_ID, ANJU_ID, LOVER_ROMANTIC);

                // then
                assertThat(pairingSelectionSummaryJpaRepository.findAll())
                        .extracting(PairingSelectionSummaryJpaEntity::getSelectionCount)
                        .containsExactly(2);
            }

            @Test
            @DisplayName("분위기를 고르지 않은 상황도 하나의 상황으로 세어 행을 새로 만들지 않는다")
            void shouldCountSituationWithoutMoodAsOneSituation() {
                // given
                pairingSelectionSummaryRepository.accumulate(DRINK_ID, ANJU_ID, BOSS_WITHOUT_MOOD);

                // when
                pairingSelectionSummaryRepository.accumulate(DRINK_ID, ANJU_ID, BOSS_WITHOUT_MOOD);

                // then
                assertThat(pairingSelectionSummaryJpaRepository.findAll())
                        .extracting(
                                PairingSelectionSummaryJpaEntity::getMoodType,
                                PairingSelectionSummaryJpaEntity::getSelectionCount)
                        .containsExactly(tuple(null, 2));
            }

            @Test
            @DisplayName("상황이 다르면 같은 조합이어도 따로 센다")
            void shouldCountEachSituationSeparately() {
                // given
                pairingSelectionSummaryRepository.accumulate(DRINK_ID, ANJU_ID, LOVER_ROMANTIC);

                // when
                pairingSelectionSummaryRepository.accumulate(DRINK_ID, ANJU_ID, BOSS_WITHOUT_MOOD);

                // then
                assertThat(pairingSelectionSummaryJpaRepository.findAll())
                        .extracting(
                                PairingSelectionSummaryJpaEntity::getRelationshipType,
                                PairingSelectionSummaryJpaEntity::getSelectionCount)
                        .containsExactlyInAnyOrder(
                                tuple(RelationshipType.LOVER, 1), tuple(RelationshipType.BOSS, 1));
            }
        }

        // 집계가 없는 상황에 두 트랜잭션이 동시에 처음 더해도 INSERT 충돌을 UPDATE로 넘겨 횟수를 잃지 않는지 본다.
        // 트랜잭션마다 커밋해야 하므로 테스트 트랜잭션을 쓰지 않고, 만든 집계를 직접 지운다.
        @Nested
        @DisplayName("같은 상황과 조합에 동시에 더하는 경우")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        class WhenAccumulatedConcurrently {

            private static final int ACCUMULATIONS_PER_THREAD = 20;

            @BeforeEach
            void setUp() {
                deleteSummary();
            }

            @AfterEach
            void tearDown() {
                deleteSummary();
            }

            @Test
            @DisplayName("모든 선택을 빠짐없이 더한다")
            void shouldKeepAllSelections() {
                // when
                List<Throwable> failures =
                        runConcurrently(this::accumulateRepeatedly, this::accumulateRepeatedly);

                // then
                assertThat(failures).isEmpty();
                assertThat(pairingSelectionSummaryJpaRepository.findAll())
                        .extracting(PairingSelectionSummaryJpaEntity::getSelectionCount)
                        .containsExactly(2 * ACCUMULATIONS_PER_THREAD);
            }

            private void accumulateRepeatedly() {
                IntStream.range(0, ACCUMULATIONS_PER_THREAD)
                        .forEach(
                                _ ->
                                        transactionTemplate.executeWithoutResult(
                                                _ ->
                                                        pairingSelectionSummaryRepository
                                                                .accumulate(
                                                                        DRINK_ID,
                                                                        ANJU_ID,
                                                                        LOVER_ROMANTIC)));
            }

            private void deleteSummary() {
                jdbcTemplate.update(
                        "DELETE FROM pairing_selection_summary WHERE drink_id = ? AND anju_id = ?",
                        DRINK_ID,
                        ANJU_ID);
            }
        }
    }
}
