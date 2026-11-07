package com.symphonia.pairing.infrastructure.jpa;

import static com.symphonia.common.helper.ConcurrencyHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.catalog.fixture.AnjuFixture;
import com.symphonia.catalog.fixture.DrinkFixture;
import com.symphonia.catalog.fixture.DrinkStyleFixture;
import com.symphonia.catalog.fixture.MusicMoodFixture;
import com.symphonia.catalog.infrastructure.jpa.AnjuJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.AnjuJpaRepository;
import com.symphonia.catalog.infrastructure.jpa.DrinkJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.DrinkJpaRepository;
import com.symphonia.catalog.infrastructure.jpa.DrinkStyleJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.DrinkStyleJpaRepository;
import com.symphonia.catalog.infrastructure.jpa.MusicMoodJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.MusicMoodJpaRepository;
import com.symphonia.pairing.domain.repository.PairingRatingSummaryRepository;
import com.symphonia.pairing.domain.vo.CombinationId;
import com.symphonia.pairing.domain.vo.PairingRatingChange;
import com.symphonia.pairing.domain.vo.PairingRatingSummary;
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

    @Autowired private PairingRatingSummaryRepository pairingRatingSummaryRepository;
    @Autowired private PairingRatingSummaryJpaRepository pairingRatingSummaryJpaRepository;
    @Autowired private DrinkStyleJpaRepository drinkStyleJpaRepository;
    @Autowired private DrinkJpaRepository drinkJpaRepository;
    @Autowired private AnjuJpaRepository anjuJpaRepository;
    @Autowired private MusicMoodJpaRepository musicMoodJpaRepository;
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
                        savedCombinations(MusicMoodFixture.FORMAL_JAZZ).getFirst();

                // when
                pairingRatingSummaryRepository.accumulate(
                        combination.drinkId(),
                        combination.anjuId(),
                        combination.musicMoodId(),
                        LIKE_ADDED);

                // then
                assertThat(summaryOf(combination))
                        .isEqualTo(
                                PairingRatingSummary.of(
                                        combination.drinkId(),
                                        combination.anjuId(),
                                        combination.musicMoodId(),
                                        1,
                                        0));
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
                        savedCombinations(MusicMoodFixture.FORMAL_JAZZ).getFirst();
                accumulate(combination, LIKE_ADDED, 2);

                // when
                pairingRatingSummaryRepository.accumulate(
                        combination.drinkId(),
                        combination.anjuId(),
                        combination.musicMoodId(),
                        new PairingRatingChange(-1, 1));

                // then
                assertThat(summaryOf(combination))
                        .isEqualTo(
                                PairingRatingSummary.of(
                                        combination.drinkId(),
                                        combination.anjuId(),
                                        combination.musicMoodId(),
                                        1,
                                        1));
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
                                jdbcTemplate.queryForObject("SELECT MIN(id) FROM anju", Long.class),
                                jdbcTemplate.queryForObject(
                                        "SELECT MIN(id) FROM music_mood", Long.class));
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
                        "DELETE FROM pairing_rating_summary WHERE drink_id = ? AND anju_id = ? AND music_mood_id = ?",
                        combination.drinkId(),
                        combination.anjuId(),
                        combination.musicMoodId());
            }
        }
    }

    @Nested
    @DisplayName("findAllWithSampleAtLeast 메서드는")
    @Sql("/sql/clear-catalog.sql")
    class FindAllWithSampleAtLeast {

        @Test
        @DisplayName("좋아요와 싫어요를 합친 표본 수가 기준 이상인 집계만 반환한다")
        void shouldReturnSummariesWithEnoughSample() {
            // given
            List<CombinationId> combinations =
                    savedCombinations(
                            MusicMoodFixture.FORMAL_JAZZ, MusicMoodFixture.CASUAL_ACOUSTIC);
            CombinationId belowMinimum = combinations.get(0);
            CombinationId atMinimum = combinations.get(1);
            accumulate(belowMinimum, new PairingRatingChange(2, 2), 1);
            accumulate(atMinimum, new PairingRatingChange(2, 3), 1);

            // when
            List<PairingRatingSummary> summaries =
                    pairingRatingSummaryRepository.findAllWithSampleAtLeast(5);

            // then
            assertThat(summaries)
                    .containsExactly(
                            PairingRatingSummary.of(
                                    atMinimum.drinkId(),
                                    atMinimum.anjuId(),
                                    atMinimum.musicMoodId(),
                                    2,
                                    3));
        }
    }

    // 음료와 안주는 하나씩 저장하고 음악만 다르게 해 조합을 만든다. 안주와 스타일 이름에 unique 제약이 있기 때문이다.
    // drink는 drink style FK를 가지므로 스타일을 먼저 저장한다.
    private List<CombinationId> savedCombinations(MusicMoodFixture... musicMoods) {
        Long drinkStyleId =
                drinkStyleJpaRepository
                        .save(DrinkStyleJpaEntity.from(DrinkStyleFixture.DISTILLED_SOJU.create()))
                        .getId();
        Long drinkId =
                drinkJpaRepository
                        .save(DrinkJpaEntity.from(DrinkFixture.SOJU.createWithStyle(drinkStyleId)))
                        .getId();
        Long anjuId =
                anjuJpaRepository
                        .save(AnjuJpaEntity.from(AnjuFixture.GOLBAENGI_MUCHIM.create()))
                        .getId();

        return Stream.of(musicMoods)
                .map(
                        musicMood ->
                                musicMoodJpaRepository.save(
                                        MusicMoodJpaEntity.from(musicMood.create())))
                .map(saved -> new CombinationId(drinkId, anjuId, saved.getId()))
                .toList();
    }

    private void accumulate(CombinationId combination, PairingRatingChange change, int times) {
        IntStream.range(0, times)
                .forEach(
                        _ ->
                                pairingRatingSummaryRepository.accumulate(
                                        combination.drinkId(),
                                        combination.anjuId(),
                                        combination.musicMoodId(),
                                        change));
    }

    private PairingRatingSummary summaryOf(CombinationId combination) {
        return pairingRatingSummaryJpaRepository
                .findById(
                        new PairingRatingSummaryJpaEntity.Combination(
                                combination.drinkId(),
                                combination.anjuId(),
                                combination.musicMoodId()))
                .orElseThrow()
                .toDomain();
    }
}
