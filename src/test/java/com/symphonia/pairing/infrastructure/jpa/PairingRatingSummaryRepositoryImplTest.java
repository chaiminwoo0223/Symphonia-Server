package com.symphonia.pairing.infrastructure.jpa;

import static com.symphonia.common.helper.ConcurrencyHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.catalog.fixture.AnjuFixture;
import com.symphonia.catalog.fixture.DrinkFixture;
import com.symphonia.catalog.fixture.DrinkStyleFixture;
import com.symphonia.catalog.infrastructure.jpa.AnjuJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.AnjuJpaRepository;
import com.symphonia.catalog.infrastructure.jpa.DrinkJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.DrinkJpaRepository;
import com.symphonia.catalog.infrastructure.jpa.DrinkStyleJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.DrinkStyleJpaRepository;
import com.symphonia.pairing.domain.repository.PairingRatingSummaryRepository;
import com.symphonia.pairing.domain.vo.CombinationId;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingRatingChange;
import com.symphonia.pairing.domain.vo.PairingRatingSummary;
import com.symphonia.pairing.domain.vo.PairingSituation;
import com.symphonia.pairing.domain.vo.RelationshipType;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Import(PairingRatingSummaryRepositoryImpl.class)
class PairingRatingSummaryRepositoryImplTest extends RepositoryTest {

    private static final PairingRatingChange LIKE_ADDED = new PairingRatingChange(1, 0);
    private static final PairingSituation LOVER_ROMANTIC =
            PairingSituation.of(RelationshipType.LOVER, MoodType.ROMANTIC);
    private static final PairingSituation BOSS_WITHOUT_MOOD =
            PairingSituation.of(RelationshipType.BOSS, null);

    @Autowired private PairingRatingSummaryRepository pairingRatingSummaryRepository;

    @Autowired
    private PairingSituationRatingSummaryJpaRepository pairingSituationRatingSummaryJpaRepository;

    @Autowired private DrinkStyleJpaRepository drinkStyleJpaRepository;
    @Autowired private DrinkJpaRepository drinkJpaRepository;
    @Autowired private AnjuJpaRepository anjuJpaRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("accumulate 메서드는")
    class Accumulate {

        @Nested
        @DisplayName("조합의 집계가 없는 경우")
        @Sql("/sql/clear-catalog.sql")
        class WhenSummaryNotExists {

            @Test
            @DisplayName("받은 변화량으로 집계를 만든다")
            void shouldCreateSummaryWithChange() {
                // given
                CombinationId combination =
                        savedCombinations(AnjuFixture.GOLBAENGI_MUCHIM).getFirst();

                // when
                pairingRatingSummaryRepository.accumulate(
                        combination.drinkId(), combination.anjuId(), LIKE_ADDED);

                // then
                assertThat(summaryOf(combination))
                        .isEqualTo(
                                PairingRatingSummary.of(
                                        combination.drinkId(), combination.anjuId(), 1, 0));
            }
        }

        @Nested
        @DisplayName("조합의 집계가 있는 경우")
        @Sql("/sql/clear-catalog.sql")
        class WhenSummaryExists {

            @Test
            @DisplayName("기존 수에 변화량을 더한다")
            void shouldAddChangeToSummary() {
                // given
                CombinationId combination =
                        savedCombinations(AnjuFixture.GOLBAENGI_MUCHIM).getFirst();
                accumulate(combination, LIKE_ADDED, 2);

                // when
                pairingRatingSummaryRepository.accumulate(
                        combination.drinkId(),
                        combination.anjuId(),
                        new PairingRatingChange(-1, 1));

                // then
                assertThat(summaryOf(combination))
                        .isEqualTo(
                                PairingRatingSummary.of(
                                        combination.drinkId(), combination.anjuId(), 1, 1));
            }
        }

        // 집계가 없는 조합에 두 트랜잭션이 동시에 처음 더해도 INSERT 충돌을 UPDATE로 넘겨 수를 잃지 않는지 본다.
        // 트랜잭션마다 커밋해야 하므로 테스트 트랜잭션을 쓰지 않고, V2 시드 카탈로그를 참조한 뒤 만든 집계만 지운다.
        @Nested
        @DisplayName("같은 조합에 동시에 더하는 경우")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        class WhenAccumulatedConcurrently {

            private static final int ACCUMULATIONS_PER_THREAD = 20;

            private CombinationId combination;

            @BeforeEach
            void setUp() {
                combination =
                        new CombinationId(
                                jdbcTemplate.queryForObject(
                                        "SELECT MIN(id) FROM drink", Long.class),
                                jdbcTemplate.queryForObject(
                                        "SELECT MIN(id) FROM anju", Long.class));
                deleteSummary();
            }

            @AfterEach
            void tearDown() {
                deleteSummary();
            }

            @Test
            @DisplayName("모든 변화량을 빠짐없이 더한다")
            void shouldKeepAllChanges() {
                // when
                List<Throwable> failures =
                        runConcurrently(this::accumulateRepeatedly, this::accumulateRepeatedly);

                // then
                assertThat(failures).isEmpty();
                assertThat(summaryOf(combination).likeCount())
                        .isEqualTo(2 * ACCUMULATIONS_PER_THREAD);
            }

            private void accumulateRepeatedly() {
                IntStream.range(0, ACCUMULATIONS_PER_THREAD)
                        .forEach(
                                _ ->
                                        transactionTemplate.executeWithoutResult(
                                                _ -> accumulate(combination, LIKE_ADDED, 1)));
            }

            private void deleteSummary() {
                jdbcTemplate.update(
                        "DELETE FROM pairing_rating_summary WHERE drink_id = ? AND anju_id = ?",
                        combination.drinkId(),
                        combination.anjuId());
            }
        }
    }

    @Nested
    @DisplayName("accumulateInSituation 메서드는")
    @Sql("/sql/clear-catalog.sql")
    class AccumulateInSituation {

        @Nested
        @DisplayName("상황의 집계가 없는 경우")
        class WhenSituationSummaryNotExists {

            @Test
            @DisplayName("받은 변화량으로 집계를 만든다")
            void shouldCreateSituationSummaryWithChange() {
                // given
                CombinationId combination =
                        savedCombinations(AnjuFixture.GOLBAENGI_MUCHIM).getFirst();

                // when
                pairingRatingSummaryRepository.accumulateInSituation(
                        combination.drinkId(), combination.anjuId(), LOVER_ROMANTIC, LIKE_ADDED);

                // then
                assertThat(situationSummariesOf(LOVER_ROMANTIC))
                        .singleElement()
                        .satisfies(summary -> assertThat(countsOf(summary)).containsExactly(1, 0));
            }

            @Test
            @DisplayName("분위기가 없는 상황의 집계를 만든다")
            void shouldCreateSituationSummaryWhenMoodIsNull() {
                // given
                CombinationId combination =
                        savedCombinations(AnjuFixture.GOLBAENGI_MUCHIM).getFirst();

                // when
                pairingRatingSummaryRepository.accumulateInSituation(
                        combination.drinkId(), combination.anjuId(), BOSS_WITHOUT_MOOD, LIKE_ADDED);

                // then
                assertThat(situationSummariesOf(BOSS_WITHOUT_MOOD))
                        .singleElement()
                        .satisfies(summary -> assertThat(countsOf(summary)).containsExactly(1, 0));
            }
        }

        @Nested
        @DisplayName("상황의 집계가 있는 경우")
        class WhenSituationSummaryExists {

            @Test
            @DisplayName("기존 수에 변화량을 더한다")
            void shouldAddChangeToSituationSummary() {
                // given
                CombinationId combination =
                        savedCombinations(AnjuFixture.GOLBAENGI_MUCHIM).getFirst();
                accumulateInSituation(combination, LOVER_ROMANTIC, LIKE_ADDED, 2);

                // when
                pairingRatingSummaryRepository.accumulateInSituation(
                        combination.drinkId(),
                        combination.anjuId(),
                        LOVER_ROMANTIC,
                        new PairingRatingChange(-1, 1));

                // then
                assertThat(situationSummariesOf(LOVER_ROMANTIC))
                        .singleElement()
                        .satisfies(summary -> assertThat(countsOf(summary)).containsExactly(1, 1));
            }

            // 분위기를 고르지 않은 상황도 하나의 상황이라, 다시 더해도 새 행을 만들지 않고 같은 행에 더한다.
            @Test
            @DisplayName("분위기가 없는 상황이면 같은 행에 더한다")
            void shouldAddChangeToSameRowWhenMoodIsNull() {
                // given
                CombinationId combination =
                        savedCombinations(AnjuFixture.GOLBAENGI_MUCHIM).getFirst();
                accumulateInSituation(combination, BOSS_WITHOUT_MOOD, LIKE_ADDED, 1);

                // when
                pairingRatingSummaryRepository.accumulateInSituation(
                        combination.drinkId(), combination.anjuId(), BOSS_WITHOUT_MOOD, LIKE_ADDED);

                // then
                assertThat(situationSummariesOf(BOSS_WITHOUT_MOOD))
                        .singleElement()
                        .satisfies(summary -> assertThat(countsOf(summary)).containsExactly(2, 0));
            }
        }

        @Nested
        @DisplayName("관계 유형이나 분위기가 다른 상황의 집계가 있는 경우")
        class WhenOtherSituationSummaryExists {

            @Test
            @DisplayName("그 집계는 바꾸지 않고 새 상황의 집계를 따로 만든다")
            void shouldCreateSeparateSituationSummary() {
                // given
                CombinationId combination =
                        savedCombinations(AnjuFixture.GOLBAENGI_MUCHIM).getFirst();
                PairingSituation loverWithoutMood =
                        PairingSituation.of(RelationshipType.LOVER, null);
                accumulateInSituation(combination, LOVER_ROMANTIC, LIKE_ADDED, 1);

                // when
                pairingRatingSummaryRepository.accumulateInSituation(
                        combination.drinkId(),
                        combination.anjuId(),
                        loverWithoutMood,
                        new PairingRatingChange(0, 1));

                // then
                assertThat(situationSummariesOf(LOVER_ROMANTIC))
                        .singleElement()
                        .satisfies(summary -> assertThat(countsOf(summary)).containsExactly(1, 0));
                assertThat(situationSummariesOf(loverWithoutMood))
                        .singleElement()
                        .satisfies(summary -> assertThat(countsOf(summary)).containsExactly(0, 1));
            }
        }
    }

    @Nested
    @DisplayName("findAllWithSampleAtLeast 메서드는")
    @Sql("/sql/clear-catalog.sql")
    class FindAllWithSampleAtLeast {

        @Nested
        @DisplayName("전역 표본이 기준에 못 미치는 조합이 있는 경우")
        class WhenSampleBelowMinimum {

            @Test
            @DisplayName("좋아요와 싫어요를 합친 전역 표본 수가 기준 이상인 집계만 반환한다")
            void shouldReturnSummariesWithEnoughSample() {
                // given
                List<CombinationId> combinations =
                        savedCombinations(AnjuFixture.GOLBAENGI_MUCHIM, AnjuFixture.DRIED_SNACK);
                CombinationId belowMinimum = combinations.get(0);
                CombinationId atMinimum = combinations.get(1);
                accumulate(belowMinimum, new PairingRatingChange(2, 2), 1);
                accumulate(atMinimum, new PairingRatingChange(2, 3), 1);

                // when
                List<PairingRatingSummary> summaries =
                        pairingRatingSummaryRepository.findAllWithSampleAtLeast(5, LOVER_ROMANTIC);

                // then
                assertThat(summaries)
                        .containsExactly(
                                PairingRatingSummary.of(
                                        atMinimum.drinkId(), atMinimum.anjuId(), 2, 3));
            }

            @Test
            @DisplayName("전역 표본이 모자란 조합은 상황별 수가 많아도 담지 않는다")
            void shouldNotReturnCombinationBelowMinimumGlobalSample() {
                // given
                CombinationId combination =
                        savedCombinations(AnjuFixture.GOLBAENGI_MUCHIM).getFirst();
                accumulate(combination, new PairingRatingChange(1, 1), 1);
                accumulateInSituation(
                        combination, LOVER_ROMANTIC, new PairingRatingChange(10, 0), 1);

                // when
                List<PairingRatingSummary> summaries =
                        pairingRatingSummaryRepository.findAllWithSampleAtLeast(5, LOVER_ROMANTIC);

                // then
                assertThat(summaries).isEmpty();
            }
        }

        @Nested
        @DisplayName("요청한 상황의 집계가 있는 경우")
        class WhenSituationSummaryExists {

            @Test
            @DisplayName("요청한 상황에서 받은 수를 전역 수와 함께 담고, 그 상황의 피드백이 없는 조합은 상황별 수를 0으로 담는다")
            void shouldAttachSituationCounts() {
                // given
                List<CombinationId> combinations =
                        savedCombinations(AnjuFixture.GOLBAENGI_MUCHIM, AnjuFixture.DRIED_SNACK);
                CombinationId withSituation = combinations.get(0);
                CombinationId withoutSituation = combinations.get(1);
                accumulate(withSituation, new PairingRatingChange(4, 1), 1);
                accumulate(withoutSituation, new PairingRatingChange(1, 4), 1);
                accumulateInSituation(
                        withSituation, LOVER_ROMANTIC, new PairingRatingChange(1, 2), 1);

                // when
                List<PairingRatingSummary> summaries =
                        pairingRatingSummaryRepository.findAllWithSampleAtLeast(5, LOVER_ROMANTIC);

                // then
                assertThat(summaries)
                        .containsExactlyInAnyOrder(
                                PairingRatingSummary.of(
                                        withSituation.drinkId(),
                                        withSituation.anjuId(),
                                        4,
                                        1,
                                        1,
                                        2),
                                PairingRatingSummary.of(
                                        withoutSituation.drinkId(),
                                        withoutSituation.anjuId(),
                                        1,
                                        4,
                                        0,
                                        0));
            }
        }

        @Nested
        @DisplayName("요청한 상황에 분위기가 없는 경우")
        class WhenMoodIsNull {

            @Test
            @DisplayName("분위기가 없는 상황이면 분위기가 없는 상황의 수를 담는다")
            void shouldAttachCountsOfSituationWithoutMood() {
                // given
                CombinationId combination =
                        savedCombinations(AnjuFixture.GOLBAENGI_MUCHIM).getFirst();
                accumulate(combination, new PairingRatingChange(4, 1), 1);
                accumulateInSituation(
                        combination, BOSS_WITHOUT_MOOD, new PairingRatingChange(0, 3), 1);

                // when
                List<PairingRatingSummary> summaries =
                        pairingRatingSummaryRepository.findAllWithSampleAtLeast(
                                5, BOSS_WITHOUT_MOOD);

                // then
                assertThat(summaries)
                        .containsExactly(
                                PairingRatingSummary.of(
                                        combination.drinkId(), combination.anjuId(), 4, 1, 0, 3));
            }
        }

        @Nested
        @DisplayName("다른 상황이나 다른 음료의 집계가 있는 경우")
        class WhenOtherSummaryExists {

            @Test
            @DisplayName("다른 상황의 수는 담지 않는다")
            void shouldNotAttachCountsOfOtherSituations() {
                // given
                CombinationId combination =
                        savedCombinations(AnjuFixture.GOLBAENGI_MUCHIM).getFirst();
                accumulate(combination, new PairingRatingChange(4, 1), 1);
                accumulateInSituation(
                        combination, LOVER_ROMANTIC, new PairingRatingChange(3, 0), 1);
                accumulateInSituation(
                        combination, BOSS_WITHOUT_MOOD, new PairingRatingChange(0, 3), 1);

                // when
                List<PairingRatingSummary> summaries =
                        pairingRatingSummaryRepository.findAllWithSampleAtLeast(
                                5, PairingSituation.of(RelationshipType.LOVER, null));

                // then
                assertThat(summaries)
                        .containsExactly(
                                PairingRatingSummary.of(
                                        combination.drinkId(), combination.anjuId(), 4, 1, 0, 0));
            }

            @Test
            @DisplayName("관계 유형이나 분위기 중 하나라도 다른 상황의 수는 담지 않는다")
            void shouldNotAttachCountsOfSituationsDifferentInRelationshipOrMood() {
                // given
                CombinationId combination =
                        savedCombinations(AnjuFixture.GOLBAENGI_MUCHIM).getFirst();
                accumulate(combination, new PairingRatingChange(4, 1), 1);
                accumulateInSituation(
                        combination,
                        PairingSituation.of(RelationshipType.LOVER, MoodType.CASUAL),
                        new PairingRatingChange(3, 0),
                        1);
                accumulateInSituation(
                        combination,
                        PairingSituation.of(RelationshipType.FRIEND, MoodType.ROMANTIC),
                        new PairingRatingChange(0, 3),
                        1);
                accumulateInSituation(
                        combination,
                        PairingSituation.of(RelationshipType.LOVER, null),
                        new PairingRatingChange(2, 2),
                        1);

                // when
                List<PairingRatingSummary> summaries =
                        pairingRatingSummaryRepository.findAllWithSampleAtLeast(5, LOVER_ROMANTIC);

                // then
                assertThat(summaries)
                        .containsExactly(
                                PairingRatingSummary.of(
                                        combination.drinkId(), combination.anjuId(), 4, 1, 0, 0));
            }

            @Test
            @DisplayName("같은 안주라도 다른 음료와의 조합에 쌓인 상황별 수는 담지 않는다")
            void shouldNotAttachCountsOfCombinationWithOtherDrink() {
                // given
                CombinationId combination =
                        savedCombinations(AnjuFixture.GOLBAENGI_MUCHIM).getFirst();
                Long drinkStyleId = drinkStyleJpaRepository.findAll().getFirst().getId();
                Long otherDrinkId =
                        drinkJpaRepository
                                .save(
                                        DrinkJpaEntity.from(
                                                DrinkFixture.BEER.createWithStyle(drinkStyleId)))
                                .getId();
                CombinationId otherDrinkCombination =
                        new CombinationId(otherDrinkId, combination.anjuId());
                accumulate(combination, new PairingRatingChange(4, 1), 1);
                accumulate(otherDrinkCombination, new PairingRatingChange(4, 1), 1);
                accumulateInSituation(
                        otherDrinkCombination, LOVER_ROMANTIC, new PairingRatingChange(3, 0), 1);

                // when
                List<PairingRatingSummary> summaries =
                        pairingRatingSummaryRepository.findAllWithSampleAtLeast(5, LOVER_ROMANTIC);

                // then
                assertThat(summaries)
                        .containsExactlyInAnyOrder(
                                PairingRatingSummary.of(
                                        combination.drinkId(), combination.anjuId(), 4, 1, 0, 0),
                                PairingRatingSummary.of(
                                        otherDrinkCombination.drinkId(),
                                        otherDrinkCombination.anjuId(),
                                        4,
                                        1,
                                        3,
                                        0));
            }
        }
    }

    // 음료는 하나만 저장하고 안주만 다르게 해 조합을 만든다. 스타일 이름에 unique 제약이 있기 때문이다.
    // drink는 drink style FK를 가지므로 스타일을 먼저 저장한다.
    private List<CombinationId> savedCombinations(AnjuFixture... anjus) {
        Long drinkStyleId =
                drinkStyleJpaRepository
                        .save(DrinkStyleJpaEntity.from(DrinkStyleFixture.DISTILLED_SOJU.create()))
                        .getId();
        Long drinkId =
                drinkJpaRepository
                        .save(DrinkJpaEntity.from(DrinkFixture.SOJU.createWithStyle(drinkStyleId)))
                        .getId();

        return Stream.of(anjus)
                .map(anju -> anjuJpaRepository.save(AnjuJpaEntity.from(anju.create())))
                .map(saved -> new CombinationId(drinkId, saved.getId()))
                .toList();
    }

    private void accumulate(CombinationId combination, PairingRatingChange change, int times) {
        IntStream.range(0, times)
                .forEach(
                        _ ->
                                pairingRatingSummaryRepository.accumulate(
                                        combination.drinkId(), combination.anjuId(), change));
    }

    private void accumulateInSituation(
            CombinationId combination,
            PairingSituation situation,
            PairingRatingChange change,
            int times) {
        IntStream.range(0, times)
                .forEach(
                        _ ->
                                pairingRatingSummaryRepository.accumulateInSituation(
                                        combination.drinkId(),
                                        combination.anjuId(),
                                        situation,
                                        change));
    }

    private List<PairingSituationRatingSummaryJpaEntity> situationSummariesOf(
            PairingSituation situation) {
        return pairingSituationRatingSummaryJpaRepository.findAll().stream()
                .filter(summary -> summary.getRelationshipType() == situation.relationshipType())
                .filter(summary -> summary.getMoodType() == situation.moodType())
                .toList();
    }

    private List<Integer> countsOf(PairingSituationRatingSummaryJpaEntity summary) {
        return List.of(summary.getLikeCount(), summary.getDislikeCount());
    }

    private PairingRatingSummary summaryOf(CombinationId combination) {
        return pairingRatingSummaryRepository.findAllWithSampleAtLeast(0, LOVER_ROMANTIC).stream()
                .filter(
                        summary ->
                                summary.drinkId().equals(combination.drinkId())
                                        && summary.anjuId().equals(combination.anjuId()))
                .findFirst()
                .orElseThrow();
    }
}
