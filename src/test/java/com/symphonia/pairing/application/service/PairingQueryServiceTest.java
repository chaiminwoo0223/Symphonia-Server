package com.symphonia.pairing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.pairing.application.dto.query.RecommendPairingQuery;
import com.symphonia.pairing.application.dto.result.PairingResult;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.MusicMood;
import com.symphonia.pairing.domain.repository.AnjuRepository;
import com.symphonia.pairing.domain.repository.DrinkRepository;
import com.symphonia.pairing.domain.repository.MusicMoodRepository;
import com.symphonia.pairing.domain.vo.AllergyType;
import com.symphonia.pairing.domain.vo.AttendeeConstraint;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import com.symphonia.pairing.domain.vo.Harmonize;
import com.symphonia.pairing.domain.vo.MoodProfile;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.Occasion;
import com.symphonia.pairing.domain.vo.PairingReason;
import com.symphonia.pairing.domain.vo.RelationshipType;
import com.symphonia.pairing.fixture.AnjuFixture;
import com.symphonia.pairing.fixture.DrinkFixture;
import com.symphonia.pairing.fixture.MusicMoodFixture;
import com.symphonia.pairing.fixture.RecommendPairingQueryFixture;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("PairingQueryService 단위 테스트")
class PairingQueryServiceTest extends UnitTest {

    @InjectMocks private PairingQueryService pairingQueryService;

    @Mock private DrinkRepository drinkRepository;
    @Mock private AnjuRepository anjuRepository;
    @Mock private MusicMoodRepository musicMoodRepository;

    @Nested
    @DisplayName("recommend 메서드는")
    class Recommend {
        // 골든 세트 조합의 안주가 들어야 하는 순위다. 추천 결과가 최대 5개라 그보다 좁게 잡는다.
        private static final int GOLDEN_TOP_RANK = 3;

        @Test
        @DisplayName("후보 조합 중 점수가 높은 순으로 PairingResult 목록을 반환한다")
        void shouldReturnPairingResultsSortedByScore() {
            // given
            givenStandardCatalog();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results).isNotEmpty();
            assertThat(results).isSortedAccordingTo((a, b) -> Double.compare(b.score(), a.score()));
        }

        @Test
        @DisplayName("상위 5개까지만 반환한다")
        void shouldLimitToTopFive() {
            // given
            givenStandardCatalog();
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture()
                            .relationshipType(RelationshipType.BOSS)
                            .moodType(MoodType.FORMAL)
                            .build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results).hasSize(5);
        }

        @Test
        @DisplayName("Occasion의 도수 상한을 초과하는 Drink는 추천에서 제외한다")
        void shouldExcludeDrinkExceedingMaxAbv() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.BEER.createWithId(),
                                    DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.DRIED_SNACK.createWithId()));
            given(musicMoodRepository.findAll())
                    .willReturn(List.of(MusicMoodFixture.FORMAL_JAZZ.createWithId()));
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture()
                            .relationshipType(RelationshipType.BOSS)
                            .build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results)
                    .extracting(PairingResult::drinkName)
                    .containsOnly(DrinkFixture.BEER.getName());
        }

        @Test
        @DisplayName("Occasion의 도수 상한으로 Drink를 조회한다")
        void shouldFindDrinksByOccasionMaxAbv() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.BEER.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.DRIED_SNACK.createWithId()));
            given(musicMoodRepository.findAll())
                    .willReturn(List.of(MusicMoodFixture.FORMAL_JAZZ.createWithId()));
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture()
                            .relationshipType(RelationshipType.BOSS)
                            .build();

            // when
            pairingQueryService.recommend(query);

            // then
            then(drinkRepository)
                    .should()
                    .findAllByAbvLessThanEqual(RelationshipType.BOSS.maxAbv());
        }

        @Test
        @DisplayName("prefersLightAnju가 true면 richness가 임계값을 초과하는 안주의 점수가 낮아진다")
        void shouldPenalizeHeavyAnjuWhenPrefersLightAnju() {
            // given
            RelationshipType relationshipType = RelationshipType.FRIEND;
            MoodType moodType = MoodType.FORMAL;
            Occasion occasion = Occasion.of(relationshipType, moodType, Set.of(), Set.of());
            MusicMood musicMood =
                    MusicMoodFixture.FORMAL_JAZZ.createWithMoodProfile(occasion.toMoodProfile());
            // 산미가 없는 와인은 맛 유사도상 후라이드치킨(0.700)이 과일안주(0.576)보다 가까워서, 페널티가 없으면 치킨이 1위가 된다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.WINE.createWithFlavorProfile(
                                            FlavorProfile.of(3, 2, 0, 3, 0))));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.FRUIT_PLATTER.createWithId(),
                                    AnjuFixture.FRIED_CHICKEN.createWithId()));
            given(musicMoodRepository.findAll()).willReturn(List.of(musicMood));
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture()
                            .relationshipType(relationshipType)
                            .moodType(moodType)
                            .build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            PairingResult fruitResult =
                    results.stream()
                            .filter(r -> r.anjuName().equals(AnjuFixture.FRUIT_PLATTER.getName()))
                            .findFirst()
                            .orElseThrow();
            PairingResult chickenResult =
                    results.stream()
                            .filter(r -> r.anjuName().equals(AnjuFixture.FRIED_CHICKEN.getName()))
                            .findFirst()
                            .orElseThrow();

            assertThat(chickenResult.score()).isLessThan(fruitResult.score());
            assertThat(results.getFirst().anjuName())
                    .isEqualTo(AnjuFixture.FRUIT_PLATTER.getName());
        }

        @Test
        @DisplayName("도수가 HIGH_ABV_THRESHOLD를 초과하는 Drink는 맛이 더 가까워도 이하인 Drink보다 점수가 낮다")
        void shouldScoreLowerWhenAbvExceedsModerateThreshold() {
            // given
            // 위스키는 후라이드치킨과 맛이 소주보다 더 가깝지만 고도수 페널티로 뒤집힌다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.SOJU.createWithId(),
                                    DrinkFixture.WHISKEY.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.FRIED_CHICKEN.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            PairingResult sojuResult =
                    results.stream()
                            .filter(r -> r.drinkName().equals(DrinkFixture.SOJU.getName()))
                            .findFirst()
                            .orElseThrow();
            PairingResult whiskeyResult =
                    results.stream()
                            .filter(r -> r.drinkName().equals(DrinkFixture.WHISKEY.getName()))
                            .findFirst()
                            .orElseThrow();

            assertThat(whiskeyResult.score()).isLessThan(sojuResult.score());
        }

        @Test
        @DisplayName("감점으로 점수가 0 아래로 내려가면 덜 나쁜 조합을 먼저 두고 점수는 0으로 반환한다")
        void shouldRankByRawScoreAndReturnZeroWhenScoresFallBelowZero() {
            // given
            // 맛이 먼 위스키는 고도수 감점, 두 안주는 FORMAL 자리의 무거운 안주 감점을 받는다.
            // 무게감 4 안주가 받는 도수 대비 점수와 자리와 먼 음악의 점수를 더해도 둘 다 0 아래로 내려간다.
            // 수육이 위스키와 맛이 조금 더 가깝지만, 0으로 자른 뒤 순위를 매기면 id가 작은 새우튀김이 먼저 온다.
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.WHISKEY.createWithFlavorProfile(
                                            FlavorProfile.of(0, 5, 5, 0, 5))));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.FRIED_SHRIMP.createWithId(),
                                    AnjuFixture.BOILED_PORK.createWithId()));
            given(musicMoodRepository.findAll())
                    .willReturn(
                            List.of(
                                    MusicMoodFixture.CASUAL_ACOUSTIC.createWithMoodProfile(
                                            MoodProfile.of(0, 5, 5, 5))));
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture().moodType(MoodType.FORMAL).build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results)
                    .extracting(PairingResult::anjuName)
                    .containsExactly(
                            AnjuFixture.BOILED_PORK.getName(), AnjuFixture.FRIED_SHRIMP.getName());
            assertThat(results).extracting(PairingResult::score).containsOnly(0.0);
        }

        @Test
        @DisplayName("attendeeConstraints에 DRIVER가 포함되면 상위에서 밀린 무알코올 Drink도 결과에 포함한다")
        void shouldIncludeNonAlcoholicPairingWhenAttendeeRequiresNonAlcoholicOption() {
            // given
            givenAlcoholicDrinksOutrankingNonAlcoholicDrink();
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture()
                            .attendeeConstraints(AttendeeConstraint.DRIVER)
                            .build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results).hasSize(5);
            assertThat(results.getLast().drinkName()).isEqualTo(DrinkFixture.SODA.getName());
            assertThat(results.getLast().drinkNonAlcoholic()).isTrue();
        }

        @Test
        @DisplayName("attendeeConstraints가 비어 있으면 무알코올 Drink를 끌어올리지 않는다")
        void shouldNotIncludeNonAlcoholicPairingWhenNoAttendeeConstraints() {
            // given
            givenAlcoholicDrinksOutrankingNonAlcoholicDrink();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results).extracting(PairingResult::drinkNonAlcoholic).containsOnly(false);
        }

        @Test
        @DisplayName("같은 스타일의 Drink가 여러 개여도 상위 결과에는 그 스타일의 조합이 하나만 남는다")
        void shouldKeepOnePairingPerDrinkStyleWhenDrinksShareStyle() {
            // given
            // 같은 스타일을 공유하는 소주 3개는 마른안주와의 맛 유사도가 가장 높아 나머지 Drink보다 점수가 높다
            List<Long> sameStyleSojuIds = List.of(11L, 12L, 13L);
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.SOJU.createWithIdAndStyle(11L, 100L),
                                    DrinkFixture.SOJU.createWithIdAndStyle(12L, 100L),
                                    DrinkFixture.SOJU.createWithIdAndStyle(13L, 100L),
                                    DrinkFixture.BEER.createWithId(),
                                    DrinkFixture.WINE.createWithId(),
                                    DrinkFixture.SODA.createWithId(),
                                    DrinkFixture.FRUIT_JUICE.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.DRIED_SNACK.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results).hasSize(5);
            assertThat(results)
                    .extracting(PairingResult::drinkId)
                    .filteredOn(sameStyleSojuIds::contains)
                    .hasSize(1);
        }

        @Test
        @DisplayName("스타일 수가 상한보다 적으면 모든 스타일을 포함하고 남은 자리를 점수 순으로 채운다")
        void shouldFillRemainingSlotsByScoreWhenDrinkStylesFewerThanLimit() {
            // given
            // 맛이 먼 위스키는 고도수 감점까지 받아 최고 조합도 소주의 5번째 조합(골뱅이무침)보다 점수가 낮다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.SOJU.createWithId(),
                                    DrinkFixture.WHISKEY.createWithFlavorProfile(
                                            FlavorProfile.of(5, 5, 5, 0, 5))));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.FRIED_CHICKEN.createWithId(),
                                    AnjuFixture.DRIED_SNACK.createWithId(),
                                    AnjuFixture.BOILED_PORK.createWithId(),
                                    AnjuFixture.CHEESE_PLATTER.createWithId(),
                                    AnjuFixture.GOLBAENGI_MUCHIM.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results).hasSize(5);
            assertThat(results)
                    .extracting(PairingResult::drinkName)
                    .containsOnlyOnce(DrinkFixture.WHISKEY.getName());
            assertThat(results)
                    .filteredOn(result -> result.drinkName().equals(DrinkFixture.SOJU.getName()))
                    .extracting(PairingResult::anjuName)
                    .containsExactlyInAnyOrder(
                            AnjuFixture.FRIED_CHICKEN.getName(),
                            AnjuFixture.DRIED_SNACK.getName(),
                            AnjuFixture.BOILED_PORK.getName(),
                            AnjuFixture.CHEESE_PLATTER.getName());
            assertThat(results).isSortedAccordingTo((a, b) -> Double.compare(b.score(), a.score()));
        }

        @Test
        @DisplayName("무알코올 옵션을 보장할 때도 같은 스타일의 Drink는 하나만 남긴다")
        void shouldKeepOnePairingPerDrinkStyleWhenAttendeeRequiresNonAlcoholicOption() {
            // given
            // 소주 6개와 맥주가 모두 탄산음료보다 점수가 높고, 그중 소주 11~13은 같은 스타일을 공유한다
            Drink soda = DrinkFixture.SODA.createWithId();
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.SOJU.createWithIdAndStyle(11L, 100L),
                                    DrinkFixture.SOJU.createWithIdAndStyle(12L, 100L),
                                    DrinkFixture.SOJU.createWithIdAndStyle(13L, 100L),
                                    DrinkFixture.SOJU.createWithIdAndStyle(14L, 101L),
                                    DrinkFixture.SOJU.createWithIdAndStyle(15L, 102L),
                                    DrinkFixture.SOJU.createWithIdAndStyle(16L, 103L),
                                    DrinkFixture.BEER.createWithId(),
                                    soda));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.DRIED_SNACK.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture()
                            .attendeeConstraints(AttendeeConstraint.DRIVER)
                            .build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results)
                    .extracting(PairingResult::drinkId)
                    .containsExactly(11L, 14L, 15L, 16L, soda.getId());
            assertThat(results.getLast().drinkNonAlcoholic()).isTrue();
        }

        @Test
        @DisplayName("이미 상위에 무알코올 Drink가 있으면 결과를 그대로 유지한다")
        void shouldKeepResultsWhenNonAlcoholicPairingAlreadyIncluded() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.SOJU.createWithId(),
                                    DrinkFixture.FRUIT_JUICE.createWithId()));
            givenSingleAnjuAndCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture()
                            .attendeeConstraints(AttendeeConstraint.PREGNANT)
                            .build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results)
                    .extracting(PairingResult::drinkName)
                    .containsExactlyInAnyOrder(
                            DrinkFixture.SOJU.getName(), DrinkFixture.FRUIT_JUICE.getName());
        }

        @Test
        @DisplayName("무알코올 Drink 후보가 없으면 기존 상위 결과를 그대로 반환한다")
        void shouldReturnTopResultsWhenNoNonAlcoholicDrinkExists() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            givenSingleAnjuAndCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture()
                            .attendeeConstraints(AttendeeConstraint.NON_DRINKER)
                            .build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results)
                    .extracting(PairingResult::drinkName)
                    .containsExactly(DrinkFixture.SOJU.getName());
        }

        @Test
        @DisplayName("attendeeAllergies와 겹치는 Anju는 추천 결과에서 제외한다")
        void shouldExcludeAnjuWhenAllergyConflicts() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.GOLBAENGI_MUCHIM.createWithId(),
                                    AnjuFixture.DRIED_SNACK.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture()
                            .attendeeAllergies(AllergyType.PEANUT)
                            .build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results)
                    .extracting(PairingResult::anjuName)
                    .containsOnly(AnjuFixture.GOLBAENGI_MUCHIM.getName());
        }

        @Test
        @DisplayName("attendeeAllergies 중 하나라도 겹치는 Anju는 모두 추천 결과에서 제외한다")
        void shouldExcludeAnjuWhenAnyOfMultipleAllergiesConflicts() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.FRIED_CHICKEN.createWithId(),
                                    AnjuFixture.FRIED_SHRIMP.createWithId(),
                                    AnjuFixture.BOILED_PORK.createWithId(),
                                    AnjuFixture.FRENCH_FRIES.createWithId(),
                                    AnjuFixture.FRUIT_PLATTER.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture()
                            .attendeeAllergies(AllergyType.PORK, AllergyType.SHRIMP)
                            .build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results)
                    .extracting(PairingResult::anjuName)
                    .containsExactlyInAnyOrder(
                            AnjuFixture.FRIED_CHICKEN.getName(),
                            AnjuFixture.FRENCH_FRIES.getName(),
                            AnjuFixture.FRUIT_PLATTER.getName());
        }

        @Test
        @DisplayName("attendeeAllergies로 모든 Anju가 제외되면 예외 없이 빈 목록을 반환한다")
        void shouldReturnEmptyListWhenAnjusExcludedByAllergies() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.GOLBAENGI_MUCHIM.createWithId(),
                                    AnjuFixture.FRIED_CHICKEN.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture().attendeeAllergies(AllergyType.WHEAT).build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Drink의 id가 null이면 예외를 던진다")
        void shouldThrowNullPointerExceptionWhenDrinkIdIsNull() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.create()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.DRIED_SNACK.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when & then
            assertThatThrownBy(() -> pairingQueryService.recommend(query))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Drink");
        }

        @Test
        @DisplayName("Anju의 id가 null이면 예외를 던진다")
        void shouldThrowNullPointerExceptionWhenAnjuIdIsNull() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll()).willReturn(List.of(AnjuFixture.DRIED_SNACK.create()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when & then
            assertThatThrownBy(() -> pairingQueryService.recommend(query))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Anju");
        }

        @Test
        @DisplayName("MusicMood의 id가 null이면 예외를 던진다")
        void shouldThrowNullPointerExceptionWhenMusicMoodIdIsNull() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.DRIED_SNACK.createWithId()));
            given(musicMoodRepository.findAll())
                    .willReturn(List.of(MusicMoodFixture.CASUAL_ACOUSTIC.create()));
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when & then
            assertThatThrownBy(() -> pairingQueryService.recommend(query))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("MusicMood");
        }

        @Test
        @DisplayName("동점 후보가 있어도 Repository 조회 순서가 바뀌면 추천 결과가 같다")
        void shouldReturnSameResultsWhenRepositoryOrderChanges() {
            // given
            // 수육과 감자튀김은 flavorProfile이 같아 같은 Drink, MusicMood와 항상 같은 점수가 나온다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.BOILED_PORK.createWithId(),
                                    AnjuFixture.FRENCH_FRIES.createWithId()))
                    .willReturn(
                            List.of(
                                    AnjuFixture.FRENCH_FRIES.createWithId(),
                                    AnjuFixture.BOILED_PORK.createWithId()));
            given(musicMoodRepository.findAll())
                    .willReturn(
                            List.of(
                                    MusicMoodFixture.FORMAL_JAZZ.createWithId(),
                                    MusicMoodFixture.CASUAL_ACOUSTIC.createWithId(),
                                    MusicMoodFixture.CELEBRATORY_DANCE.createWithId()))
                    .willReturn(
                            List.of(
                                    MusicMoodFixture.CELEBRATORY_DANCE.createWithId(),
                                    MusicMoodFixture.CASUAL_ACOUSTIC.createWithId(),
                                    MusicMoodFixture.FORMAL_JAZZ.createWithId()));
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> resultsInOriginalOrder = pairingQueryService.recommend(query);
            List<PairingResult> resultsInReversedOrder = pairingQueryService.recommend(query);

            // then
            assertThat(resultsInOriginalOrder).hasSize(5);
            assertThat(resultsInReversedOrder).isEqualTo(resultsInOriginalOrder);
        }

        @Test
        @DisplayName("Drink 점수가 같으면 id가 작은 순서로 반환한다")
        void shouldOrderByDrinkIdAscendingWhenScoresTie() {
            // given
            // 맥주에 소주의 flavorProfile을 주어 두 Drink의 점수를 같게 만든다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.BEER.createWithFlavorProfile(
                                            DrinkFixture.SOJU.getFlavorProfile()),
                                    DrinkFixture.SOJU.createWithId(1L)));
            givenSingleAnjuAndCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.get(0).score()).isEqualTo(results.get(1).score());
            assertThat(results)
                    .extracting(PairingResult::drinkName)
                    .containsExactly(DrinkFixture.SOJU.getName(), DrinkFixture.BEER.getName());
        }

        @Test
        @DisplayName("Anju 점수가 같으면 id가 작은 순서로 반환한다")
        void shouldOrderByAnjuIdAscendingWhenScoresTie() {
            // given
            // 수육과 감자튀김은 flavorProfile이 같아 점수가 같다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.FRENCH_FRIES.createWithId(2L),
                                    AnjuFixture.BOILED_PORK.createWithId(1L)));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.get(0).score()).isEqualTo(results.get(1).score());
            assertThat(results)
                    .extracting(PairingResult::anjuName)
                    .containsExactly(
                            AnjuFixture.BOILED_PORK.getName(), AnjuFixture.FRENCH_FRIES.getName());
        }

        @Test
        @DisplayName("MusicMood 점수가 같으면 id가 작은 순서로 반환한다")
        void shouldOrderByMusicMoodIdAscendingWhenScoresTie() {
            // given
            // 파티 댄스에 재즈의 moodProfile을 주어 두 MusicMood의 점수를 같게 만든다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.TOFU_KIMCHI.createWithId()));
            given(musicMoodRepository.findAll())
                    .willReturn(
                            List.of(
                                    MusicMoodFixture.CELEBRATORY_DANCE.createWithMoodProfile(
                                            MusicMoodFixture.FORMAL_JAZZ.getMoodProfile()),
                                    MusicMoodFixture.FORMAL_JAZZ.createWithId(1L)));
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.get(0).score()).isEqualTo(results.get(1).score());
            assertThat(results)
                    .extracting(PairingResult::musicMoodTitle)
                    .containsExactly(
                            MusicMoodFixture.FORMAL_JAZZ.getTitle(),
                            MusicMoodFixture.CELEBRATORY_DANCE.getTitle());
        }

        @Test
        @DisplayName("Occasion에 도수 제한이 있으면 reasons에 ABV_LIMIT을 포함한다")
        void shouldIncludeAbvLimitReasonWhenOccasionHasAbvLimit() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.BEER.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.DRIED_SNACK.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture()
                            .relationshipType(RelationshipType.BOSS)
                            .build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons()).contains(PairingReason.ABV_LIMIT.message());
        }

        @Test
        @DisplayName("Occasion에 도수 제한이 없으면 reasons에 ABV_LIMIT을 포함하지 않는다")
        void shouldNotIncludeAbvLimitReasonWhenOccasionHasNoAbvLimit() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.BEER.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.DRIED_SNACK.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture()
                            .relationshipType(RelationshipType.FRIEND)
                            .build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .doesNotContain(PairingReason.ABV_LIMIT.message());
        }

        @Test
        @DisplayName("prefersLightAnju가 true고 richness가 임계값 이하면 reasons에 LIGHT_ANJU를 포함한다")
        void shouldIncludeLightAnjuReasonWhenAnjuRichnessWithinThreshold() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.GOLBAENGI_MUCHIM.createWithId()));
            given(musicMoodRepository.findAll())
                    .willReturn(List.of(MusicMoodFixture.FORMAL_JAZZ.createWithId()));
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture().moodType(MoodType.FORMAL).build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons()).contains(PairingReason.LIGHT_ANJU.message());
        }

        @Test
        @DisplayName("prefersLightAnju가 true여도 richness가 임계값을 초과하면 reasons에 LIGHT_ANJU를 포함하지 않는다")
        void shouldNotIncludeLightAnjuReasonWhenAnjuRichnessExceedsThreshold() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.FRIED_CHICKEN.createWithId()));
            given(musicMoodRepository.findAll())
                    .willReturn(List.of(MusicMoodFixture.FORMAL_JAZZ.createWithId()));
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture().moodType(MoodType.FORMAL).build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .doesNotContain(PairingReason.LIGHT_ANJU.message());
        }

        @Test
        @DisplayName("무알코올 옵션이 필요하고 Drink가 무알코올이면 reasons에 NON_ALCOHOLIC_OPTION을 포함한다")
        void shouldIncludeNonAlcoholicOptionReasonWhenDrinkIsNonAlcoholic() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SODA.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.DRIED_SNACK.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture()
                            .attendeeConstraints(AttendeeConstraint.DRIVER)
                            .build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .contains(PairingReason.NON_ALCOHOLIC_OPTION.message());
        }

        @Test
        @DisplayName("무알코올 옵션이 필요해도 Drink가 무알코올이 아니면 reasons에 NON_ALCOHOLIC_OPTION을 포함하지 않는다")
        void shouldNotIncludeNonAlcoholicOptionReasonWhenDrinkIsAlcoholic() {
            // given
            givenSojuAndDriedSnackWithCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture()
                            .attendeeConstraints(AttendeeConstraint.DRIVER)
                            .build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .doesNotContain(PairingReason.NON_ALCOHOLIC_OPTION.message());
        }

        @Test
        @DisplayName("attendeeAllergies가 있으면 살아남은 결과의 reasons에 ALLERGY_EXCLUDED를 포함한다")
        void shouldIncludeAllergyExcludedReasonWhenAttendeeAllergiesPresent() {
            // given
            givenSojuAndDriedSnackWithCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture().attendeeAllergies(AllergyType.WHEAT).build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .contains(PairingReason.ALLERGY_EXCLUDED.message());
        }

        @Test
        @DisplayName("attendeeAllergies가 없으면 reasons에 ALLERGY_EXCLUDED를 포함하지 않는다")
        void shouldNotIncludeAllergyExcludedReasonWhenNoAttendeeAllergies() {
            // given
            givenSojuAndDriedSnackWithCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .doesNotContain(PairingReason.ALLERGY_EXCLUDED.message());
        }

        @Test
        @DisplayName("flavorSimilarity가 임계값 이상이면 reasons에 FLAVOR_MATCH를 포함한다")
        void shouldIncludeFlavorMatchReasonWhenFlavorSimilarityAboveThreshold() {
            // given
            // 소주와 마른안주는 맛 유사도가 임계값(0.7)을 넘는다
            givenSojuAndDriedSnackWithCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons()).contains(PairingReason.FLAVOR_MATCH.message());
        }

        @Test
        @DisplayName("flavorSimilarity가 임계값 미만이면 reasons에 FLAVOR_MATCH를 포함하지 않는다")
        void shouldNotIncludeFlavorMatchReasonWhenFlavorSimilarityBelowThreshold() {
            // given
            // 소주와 후라이드치킨은 맛 유사도가 임계값(0.7)에 못 미친다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.FRIED_CHICKEN.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .doesNotContain(PairingReason.FLAVOR_MATCH.message());
        }

        @Test
        @DisplayName("나머지 맛 축이 같아도 산미 차이가 크면 reasons에 FLAVOR_MATCH를 포함하지 않는다")
        void shouldNotIncludeFlavorMatchReasonWhenOnlyAcidityDiffers() {
            // given
            // 마른안주와 산미(0 vs 5)만 다른 Drink는 맛 유사도가 1 - sqrt(25 / 100) = 0.5로 임계값(0.7)에 못 미친다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.SOJU.createWithFlavorProfile(
                                            FlavorProfile.of(1, 1, 0, 1, 5))));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.DRIED_SNACK.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .doesNotContain(PairingReason.FLAVOR_MATCH.message());
        }

        @Test
        @DisplayName("탄산만 다른 Drink는 탄산 차이가 커도 reasons에 FLAVOR_MATCH를 포함한다")
        void shouldIncludeFlavorMatchReasonWhenOnlyCarbonationDiffers() {
            // given
            // 마른안주와 탄산(0 vs 5)만 다른 Drink다. 탄산은 맛 유사도에 들어가지 않아 맛 유사도가 1이다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.SOJU.createWithFlavorProfile(
                                            FlavorProfile.of(1, 1, 5, 1, 0))));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.DRIED_SNACK.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons()).contains(PairingReason.FLAVOR_MATCH.message());
        }

        @Test
        @DisplayName("moodFitness가 임계값 이상이면 reasons에 MOOD_MATCH를 포함한다")
        void shouldIncludeMoodMatchReasonWhenMoodFitnessAboveThreshold() {
            // given
            // 편안한 어쿠스틱은 CASUAL Occasion의 moodProfile과 적합도가 임계값(0.7)을 넘는다
            givenSojuAndDriedSnackWithCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture().moodType(MoodType.CASUAL).build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons()).contains(PairingReason.MOOD_MATCH.message());
        }

        @Test
        @DisplayName("moodFitness가 임계값 미만이면 reasons에 MOOD_MATCH를 포함하지 않는다")
        void shouldNotIncludeMoodMatchReasonWhenMoodFitnessBelowThreshold() {
            // given
            // 잔잔한 재즈는 CASUAL Occasion의 moodProfile과 적합도가 임계값(0.7)에 못 미친다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.DRIED_SNACK.createWithId()));
            given(musicMoodRepository.findAll())
                    .willReturn(List.of(MusicMoodFixture.FORMAL_JAZZ.createWithId()));
            RecommendPairingQuery query =
                    new RecommendPairingQueryFixture().moodType(MoodType.CASUAL).build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .doesNotContain(PairingReason.MOOD_MATCH.message());
        }

        @Test
        @DisplayName("Drink의 Harmonize에 맞는 Anju는 맛이 같아도 맞지 않는 Anju보다 점수가 높다")
        void shouldScoreHigherWhenAnjuMatchesDrinkHarmonize() {
            // given
            // 수육과 감자튀김은 맛이 같고, 음료의 음식 계열(PORK)에는 수육만 맞는다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.WINE.createWithHarmonizes(
                                            Set.of(Harmonize.PORK))));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.BOILED_PORK.createWithId(),
                                    AnjuFixture.FRENCH_FRIES.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(scoreOf(results, AnjuFixture.BOILED_PORK))
                    .isGreaterThan(scoreOf(results, AnjuFixture.FRENCH_FRIES));
        }

        @Test
        @DisplayName("도수가 15% 이상인 Drink는 무게감 높은 Anju를 맛이 더 가까운 가벼운 Anju보다 앞에 둔다")
        void shouldRankHeavyAnjuFirstWhenDrinkCutsRichness() {
            // given
            // 소주와 맛 유사도는 마른안주(0.827)가 후라이드치킨(0.542)보다 높지만, 무게감 5인 후라이드치킨이 도수 대비 점수를 받는다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.DRIED_SNACK.createWithId(),
                                    AnjuFixture.FRIED_CHICKEN.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().anjuName())
                    .isEqualTo(AnjuFixture.FRIED_CHICKEN.getName());
        }

        @Test
        @DisplayName("도수 대비 점수는 Anju의 무게감이 클수록 커진다")
        void shouldGiveLargerRichnessCuttingBonusWhenAnjuIsHeavier() {
            // given
            // 소주와 맛 유사도는 새우튀김(무게감 4, 0.626)이 후라이드치킨(무게감 5, 0.542)보다 높다.
            // 무게감에 비례한 도수 대비 점수(0.1, 0.2)를 받아야 후라이드치킨이 앞선다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.FRIED_SHRIMP.createWithId(),
                                    AnjuFixture.FRIED_CHICKEN.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().anjuName())
                    .isEqualTo(AnjuFixture.FRIED_CHICKEN.getName());
        }

        @Test
        @DisplayName("탄산이나 산미가 4 이상인 Drink는 무게감 높은 Anju를 맛이 더 가까운 가벼운 Anju보다 앞에 둔다")
        void shouldRankHeavyAnjuFirstWhenDrinkCutsRichnessWithCarbonationOrAcidity() {
            // given
            // 카스 프레시와 맛 유사도는 마른안주(0.735)가 후라이드치킨(0.564)보다 높지만, 후라이드치킨이 탄산 대비 점수를 받는다.
            // 후라이드치킨에 맞는 음식 계열(POULTRY)을 비워 대비 점수만으로 뒤집히는지 본다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.CASS_FRESH.createWithHarmonizes(Set.of())));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.DRIED_SNACK.createWithId(),
                                    AnjuFixture.FRIED_CHICKEN.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().anjuName())
                    .isEqualTo(AnjuFixture.FRIED_CHICKEN.getName());
        }

        @Test
        @DisplayName("탄산, 산미 대비 점수는 탄산이 강할수록 커진다")
        void shouldGiveLargerCarbonationAcidityCuttingBonusWhenCarbonationIsStronger() {
            // given
            // 두 음료는 탄산(4, 5)만 다르다. 탄산은 맛 유사도에 들어가지 않으므로 점수 차이는 대비 점수(0.16, 0.2)에서만 생긴다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.BEER.createWithFlavorProfile(
                                            FlavorProfile.of(2, 3, 4, 2, 1)),
                                    DrinkFixture.CASS_FRESH.createWithHarmonizes(Set.of())));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.FRIED_CHICKEN.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(scoreOfDrink(results, DrinkFixture.CASS_FRESH.getName()))
                    .isGreaterThan(scoreOfDrink(results, DrinkFixture.BEER.getName()));
        }

        @Test
        @DisplayName("도수 대비와 탄산, 산미 대비를 함께 받는 Drink는 두 점수를 더하지 않고 큰 쪽만 받는다")
        void shouldNotAddCarbonationAcidityCuttingBonusWhenDrinkAlsoCutsRichnessByAbv() {
            // given
            // 두 소주는 탄산(0, 5)만 다르다. 도수 대비(0.2)와 탄산 대비(0.2)를 더하면 탄산 5인 소주의 점수가 0.2 더 높아진다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.SOJU.createWithId(1L),
                                    DrinkFixture.SOJU.createWithIdAndFlavorProfile(
                                            2L, FlavorProfile.of(2, 2, 5, 1, 1))));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.FRIED_CHICKEN.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results)
                    .extracting(PairingResult::score)
                    .containsExactly(results.getFirst().score(), results.getFirst().score());
        }

        @Test
        @DisplayName("Anju가 Drink의 Harmonize에 맞으면 reasons에 HARMONIZE_MATCH를 포함한다")
        void shouldIncludeHarmonizeMatchReasonWhenAnjuMatchesDrinkHarmonize() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.WINE.createWithHarmonizes(
                                            Set.of(Harmonize.PORK))));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.BOILED_PORK.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .contains(PairingReason.HARMONIZE_MATCH.message());
        }

        @Test
        @DisplayName("Anju가 Drink의 Harmonize에 맞지 않으면 reasons에 HARMONIZE_MATCH를 포함하지 않는다")
        void shouldNotIncludeHarmonizeMatchReasonWhenAnjuDoesNotMatchDrinkHarmonize() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.WINE.createWithHarmonizes(
                                            Set.of(Harmonize.PORK))));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.FRENCH_FRIES.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .doesNotContain(PairingReason.HARMONIZE_MATCH.message());
        }

        @Test
        @DisplayName("도수가 15% 이상이고 Anju가 무거우면 reasons에 RICHNESS_CUTTING을 포함한다")
        void shouldIncludeRichnessCuttingReasonWhenDrinkCutsRichness() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.FRIED_CHICKEN.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .contains(PairingReason.RICHNESS_CUTTING.message());
        }

        @Test
        @DisplayName("도수가 15% 이상이어도 Anju가 가벼우면 reasons에 RICHNESS_CUTTING을 포함하지 않는다")
        void shouldNotIncludeRichnessCuttingReasonWhenAnjuIsLight() {
            // given
            givenSojuAndDriedSnackWithCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .doesNotContain(PairingReason.RICHNESS_CUTTING.message());
        }

        @Test
        @DisplayName("Anju가 무거워도 도수가 15% 미만이면 reasons에 RICHNESS_CUTTING을 포함하지 않는다")
        void shouldNotIncludeRichnessCuttingReasonWhenAbvBelowThreshold() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.BEER.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.FRIED_CHICKEN.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .doesNotContain(PairingReason.RICHNESS_CUTTING.message());
        }

        @Test
        @DisplayName("탄산이 4 이상이고 Anju가 무거우면 reasons에 CARBONATION_ACIDITY_CUTTING을 포함한다")
        void shouldIncludeCarbonationAcidityCuttingReasonWhenCarbonationIsStrong() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.CASS_FRESH.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.FRIED_CHICKEN.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .contains(PairingReason.CARBONATION_ACIDITY_CUTTING.message());
        }

        @Test
        @DisplayName("산미가 4 이상이고 Anju가 무거우면 reasons에 CARBONATION_ACIDITY_CUTTING을 포함한다")
        void shouldIncludeCarbonationAcidityCuttingReasonWhenAcidityIsStrong() {
            // given: 와인은 탄산 0, 산미 4다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.WINE.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.FRIED_CHICKEN.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .contains(PairingReason.CARBONATION_ACIDITY_CUTTING.message());
        }

        @Test
        @DisplayName("도수 대비를 받는 Drink는 탄산이 4 이상이어도 reasons에 RICHNESS_CUTTING만 포함한다")
        void shouldIncludeOnlyRichnessCuttingReasonWhenDrinkAlsoCutsRichnessByAbv() {
            // given: 도수 16.5%인 소주에 탄산 5를 준다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.SOJU.createWithFlavorProfile(
                                            FlavorProfile.of(2, 2, 5, 1, 1))));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.FRIED_CHICKEN.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .contains(PairingReason.RICHNESS_CUTTING.message())
                    .doesNotContain(PairingReason.CARBONATION_ACIDITY_CUTTING.message());
        }

        @Test
        @DisplayName("탄산이 4 이상이어도 Anju가 가벼우면 reasons에 CARBONATION_ACIDITY_CUTTING을 포함하지 않는다")
        void shouldNotIncludeCarbonationAcidityCuttingReasonWhenAnjuIsLight() {
            // given
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.CASS_FRESH.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.DRIED_SNACK.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .doesNotContain(PairingReason.CARBONATION_ACIDITY_CUTTING.message());
        }

        @Test
        @DisplayName("Anju가 무거워도 탄산과 산미가 모두 4 미만이면 reasons에 CARBONATION_ACIDITY_CUTTING을 포함하지 않는다")
        void shouldNotIncludeCarbonationAcidityCuttingReasonWhenCarbonationAndAcidityAreWeak() {
            // given: 소주는 탄산 0, 산미 1이다
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.FRIED_CHICKEN.createWithId()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.getFirst().reasons())
                    .doesNotContain(PairingReason.CARBONATION_ACIDITY_CUTTING.message());
        }

        // 삼겹살구이, 대창구이, 소막창구이처럼 맛 값이 같은 안주는 순서가 id로만 갈리므로, 안주 하나가 아니라 상위 3위의 성질을 본다.
        // 소주에 음식 계열이 없으면 맛 값이 같은 고다치즈, 팝콘 같은 안주도 고기 요리와 동점이 된다.
        // 기름진 고기 요리라는 성질은 도수 대비와 Harmonize가 함께 만들므로 두 사유를 같이 본다. 둘 중 하나를 되돌려도 실패한다.
        @Test
        @DisplayName("소주에는 기름진 고기 요리를 상위 3위로 추천한다")
        void shouldRankRichMeatDishesWithinTopThreeWhenDrinkIsSoju() {
            // given: 카탈로그의 시드 소주와 이름, 도수, 맛이 같고 V8이 넣은 음식 계열(PORK, BEEF)을 갖는다
            givenGoldenCatalog(
                    DrinkFixture.SOJU.createWithHarmonizes(Set.of(Harmonize.PORK, Harmonize.BEEF)));
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.subList(0, GOLDEN_TOP_RANK))
                    .extracting(PairingResult::reasons)
                    .allSatisfy(
                            reasons ->
                                    assertThat(reasons)
                                            .contains(
                                                    PairingReason.RICHNESS_CUTTING.message(),
                                                    PairingReason.HARMONIZE_MATCH.message()));
        }

        // X-Wines에서 이 와인의 Harmonize는 Pasta, Veal, Pork, Poultry다. 치즈 계열이 없어 치즈플래터는 기대 안주로 두지 않는다.
        // 산미가 4라 탄산, 산미 대비를 받는 대창구이, 삼겹살구이, 소막창구이가 동점 1위다. 깐풍기, 두부김치 같은 가벼운 안주는 Harmonize에만 맞는다.
        @Test
        @DisplayName("레드 와인에는 X-Wines Harmonize에 맞고 산미가 기름기를 씻어 주는 안주를 상위 3위로 추천한다")
        void shouldRankHarmonizingAnjusWithinTopThreeWhenDrinkIsRedWine() {
            // given
            givenGoldenCatalog(DrinkFixture.LOUIS_JADOT_BEAUJOLAIS.createWithId());
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results.subList(0, GOLDEN_TOP_RANK))
                    .extracting(PairingResult::reasons)
                    .allSatisfy(
                            reasons ->
                                    assertThat(reasons)
                                            .contains(
                                                    PairingReason.HARMONIZE_MATCH.message(),
                                                    PairingReason.CARBONATION_ACIDITY_CUTTING
                                                            .message()));
        }

        // 탄산, 산미 대비 이전에는 후라이드치킨이 맛 유사도 0.434로 카탈로그 173개 중 117위였다.
        // 안주의 탄산이 늘 0이라 탄산(5, 0) 차이가 거리 감점이 됐다.
        // #106 이후에는 지방 함량만 높은 팝콘, 볶음땅콩, 견과류모둠이 콜드컷플래터와 동점 1위였다.
        // 집어 먹는 안주의 무게감을 1 낮춘 뒤(#117)에는 콜드컷플래터, 도토리묵무침, 장어구이 순이고 세 스낵은 후라이드치킨과 동점 4위다.
        // 상위 3위는 모두 음식 계열에 맞고, 1위는 탄산이 기름기를 씻어 준다. 형태 보정을 되돌리면 세 스낵이 상위 3위에 들어 실패한다.
        @Test
        @DisplayName("라거에는 지방 함량만 높은 스낵보다 음식 계열에 맞는 안주를 상위 3위로 추천한다")
        void shouldRankHarmonizingAnjusOverHighFatSnacksWithinTopThreeWhenDrinkIsLager() {
            // given
            givenGoldenCatalog(DrinkFixture.CASS_FRESH.createWithId());
            RecommendPairingQuery query = new RecommendPairingQueryFixture().build();

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            List<PairingResult> topRanked = results.subList(0, GOLDEN_TOP_RANK);
            assertThat(topRanked)
                    .extracting(PairingResult::anjuName)
                    .doesNotContain(
                            AnjuFixture.CATALOG_POPCORN.getName(),
                            AnjuFixture.CATALOG_ROASTED_PEANUTS.getName(),
                            AnjuFixture.CATALOG_MIXED_NUTS.getName());
            assertThat(topRanked)
                    .extracting(PairingResult::reasons)
                    .allSatisfy(
                            reasons ->
                                    assertThat(reasons)
                                            .contains(PairingReason.HARMONIZE_MATCH.message()));
            assertThat(topRanked.getFirst().reasons())
                    .contains(PairingReason.CARBONATION_ACIDITY_CUTTING.message());
        }

        private void givenStandardCatalog() {
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.SOJU.createWithId(),
                                    DrinkFixture.BEER.createWithId(),
                                    DrinkFixture.WHISKEY.createWithId(),
                                    DrinkFixture.WINE.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.GOLBAENGI_MUCHIM.createWithId(),
                                    AnjuFixture.FRIED_CHICKEN.createWithId(),
                                    AnjuFixture.DRIED_SNACK.createWithId(),
                                    AnjuFixture.TOFU_KIMCHI.createWithId(),
                                    AnjuFixture.CHEESE_PLATTER.createWithId(),
                                    AnjuFixture.FRUIT_PLATTER.createWithId()));
            given(musicMoodRepository.findAll())
                    .willReturn(
                            List.of(
                                    MusicMoodFixture.FORMAL_JAZZ.createWithId(),
                                    MusicMoodFixture.CASUAL_ACOUSTIC.createWithId(),
                                    MusicMoodFixture.CELEBRATORY_DANCE.createWithId()));
        }

        // 스타일이 서로 다른 소주 5개가 탄산음료보다 점수가 높아, 스타일당 하나를 골라도 상위 5개가 모두 알코올이다.
        private void givenAlcoholicDrinksOutrankingNonAlcoholicDrink() {
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(
                            List.of(
                                    DrinkFixture.SOJU.createWithIdAndStyle(11L, 101L),
                                    DrinkFixture.SOJU.createWithIdAndStyle(12L, 102L),
                                    DrinkFixture.SOJU.createWithIdAndStyle(13L, 103L),
                                    DrinkFixture.SOJU.createWithIdAndStyle(14L, 104L),
                                    DrinkFixture.SOJU.createWithIdAndStyle(15L, 105L),
                                    DrinkFixture.SODA.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.DRIED_SNACK.createWithId()));
            givenCasualAcousticMusicMood();
        }

        private void givenSingleAnjuAndCasualAcousticMusicMood() {
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.TOFU_KIMCHI.createWithId()));
            givenCasualAcousticMusicMood();
        }

        private void givenCasualAcousticMusicMood() {
            given(musicMoodRepository.findAll())
                    .willReturn(List.of(MusicMoodFixture.CASUAL_ACOUSTIC.createWithId()));
        }

        // 소주와 마른안주는 도수 제한·light anju·무알코올 조건과 무관하고, 맛 유사도와 편안한 어쿠스틱과의
        // 무드 적합도만 임계값(0.7)을 넘는 조합이다.
        private void givenSojuAndDriedSnackWithCasualAcousticMusicMood() {
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(DrinkFixture.SOJU.createWithId()));
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.DRIED_SNACK.createWithId()));
            givenCasualAcousticMusicMood();
        }

        private double scoreOfDrink(List<PairingResult> results, String drinkName) {
            return results.stream()
                    .filter(result -> result.drinkName().equals(drinkName))
                    .findFirst()
                    .orElseThrow()
                    .score();
        }

        private double scoreOf(List<PairingResult> results, AnjuFixture anju) {
            return results.stream()
                    .filter(result -> result.anjuName().equals(anju.getName()))
                    .findFirst()
                    .orElseThrow()
                    .score();
        }

        // 음료와 음악을 하나씩만 두어 결과 순위가 곧 그 음료의 안주 순위가 되게 한다.
        // 안주 풀에는 골든 세트 음료별로 현재 규칙의 카탈로그 상위 안주(동점 포함)를 넣어 풀의 순위가 카탈로그 순위와 같게 한다.
        // 기대 성질에 맞지 않는 함정 안주(성질에 맞지 않는 안주 중 가장 높은 안주, 이전 규칙의 상위 안주)도 넣어 규칙이 되돌아가면 실패하게 한다.
        // 함정 안주를 앞에 두고 순서대로 id를 매겨, 동점이면 함정 안주가 먼저 오는 가장 불리한 경우를 검증한다.
        private void givenGoldenCatalog(Drink drink) {
            List<AnjuFixture> traps =
                    List.of(
                            AnjuFixture.CATALOG_GOUDA_CHEESE,
                            AnjuFixture.CATALOG_GOPCHANG_JEONGOL,
                            AnjuFixture.DRIED_SNACK,
                            AnjuFixture.CATALOG_DOTORIMUK_MUCHIM,
                            AnjuFixture.CATALOG_DAK_KKOCHI,
                            AnjuFixture.CATALOG_JJIMDAK,
                            AnjuFixture.CATALOG_SMOKED_SALMON,
                            AnjuFixture.CATALOG_BUDAE_JJIGAE,
                            AnjuFixture.CATALOG_CHEESE_PLATTER,
                            AnjuFixture.CATALOG_DAKGANGJEONG,
                            AnjuFixture.CATALOG_DAKTTONGJIP_BOKKEUM,
                            AnjuFixture.CATALOG_POPCORN,
                            AnjuFixture.CATALOG_ROASTED_PEANUTS,
                            AnjuFixture.CATALOG_MIXED_NUTS);
            List<AnjuFixture> topRanked =
                    List.of(
                            AnjuFixture.CATALOG_DWAEJI_KKEOPDEGI_GUI,
                            AnjuFixture.CATALOG_SOMAKCHANG_GUI,
                            AnjuFixture.CATALOG_DAECHANG_GUI,
                            AnjuFixture.CATALOG_SAMGYEOPSAL_GUI,
                            AnjuFixture.CATALOG_GOCHUJANG_SAMGYEOPSAL,
                            AnjuFixture.CATALOG_KKANPUNGGI,
                            AnjuFixture.CATALOG_TOFU_KIMCHI,
                            AnjuFixture.CATALOG_KIMCHI_JJIGAE,
                            AnjuFixture.CATALOG_TANGSUYUK,
                            AnjuFixture.CATALOG_FRIED_CHICKEN,
                            AnjuFixture.CATALOG_COLD_CUT_PLATTER,
                            AnjuFixture.CATALOG_JANGEO_GUI);
            List<AnjuFixture> pool = Stream.concat(traps.stream(), topRanked.stream()).toList();
            given(drinkRepository.findAllByAbvLessThanEqual(anyDouble()))
                    .willReturn(List.of(drink));
            given(anjuRepository.findAll())
                    .willReturn(
                            IntStream.range(0, pool.size())
                                    .mapToObj(index -> pool.get(index).createWithId(index + 1L))
                                    .toList());
            givenCasualAcousticMusicMood();
        }
    }
}
