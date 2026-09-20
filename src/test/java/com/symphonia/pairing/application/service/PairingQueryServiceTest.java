package com.symphonia.pairing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.symphonia.UnitTest;
import com.symphonia.pairing.application.dto.query.RecommendPairingQuery;
import com.symphonia.pairing.application.dto.result.PairingResult;
import com.symphonia.pairing.domain.entity.MusicMood;
import com.symphonia.pairing.domain.repository.AnjuRepository;
import com.symphonia.pairing.domain.repository.DrinkRepository;
import com.symphonia.pairing.domain.repository.MusicMoodRepository;
import com.symphonia.pairing.domain.vo.AllergyType;
import com.symphonia.pairing.domain.vo.AttendeeConstraint;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.Occasion;
import com.symphonia.pairing.domain.vo.RelationshipType;
import com.symphonia.pairing.fixture.AnjuFixture;
import com.symphonia.pairing.fixture.DrinkFixture;
import com.symphonia.pairing.fixture.MusicMoodFixture;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

        @Test
        @DisplayName("후보 조합 중 점수가 높은 순으로 PairingResult 목록을 반환한다")
        void shouldReturnPairingResultsSortedByScore() {
            // given
            givenStandardCatalog();
            RecommendPairingQuery query =
                    new RecommendPairingQuery(
                            RelationshipType.FRIEND, MoodType.CASUAL, Set.of(), Set.of());

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
                    new RecommendPairingQuery(
                            RelationshipType.BOSS, MoodType.FORMAL, Set.of(), Set.of());

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results).hasSize(5);
        }

        @Test
        @DisplayName("Occasion의 도수 상한을 초과하는 Drink는 추천에서 제외한다")
        void shouldExcludeDrinkExceedingMaxAbv() {
            // given
            given(drinkRepository.findAll())
                    .willReturn(List.of(DrinkFixture.BEER.create(), DrinkFixture.SOJU.create()));
            given(anjuRepository.findAll()).willReturn(List.of(AnjuFixture.DRIED_SNACK.create()));
            given(musicMoodRepository.findAll())
                    .willReturn(List.of(MusicMoodFixture.FORMAL_JAZZ.create()));
            RecommendPairingQuery query =
                    new RecommendPairingQuery(
                            RelationshipType.BOSS, MoodType.CASUAL, Set.of(), Set.of());

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results)
                    .extracting(PairingResult::drinkName)
                    .containsOnly(DrinkFixture.BEER.getName());
        }

        @Test
        @DisplayName("prefersLightAnju가 true면 richness가 임계값을 초과하는 안주의 점수가 낮아진다")
        void shouldPenalizeHeavyAnjuWhenPrefersLightAnju() {
            // given
            RelationshipType relationshipType = RelationshipType.FRIEND;
            MoodType moodType = MoodType.FORMAL;
            Occasion occasion = Occasion.of(relationshipType, moodType, Set.of(), Set.of());
            MusicMood musicMood = MusicMoodFixture.FORMAL_JAZZ.create(occasion.toMoodProfile());
            // 와인은 맛 유사도상 후라이드치킨이 과일안주보다 가까워서, 페널티가 없으면 치킨이 1위가 된다
            given(drinkRepository.findAll()).willReturn(List.of(DrinkFixture.WINE.create()));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.FRUIT_PLATTER.create(),
                                    AnjuFixture.FRIED_CHICKEN.create()));
            given(musicMoodRepository.findAll()).willReturn(List.of(musicMood));
            RecommendPairingQuery query =
                    new RecommendPairingQuery(relationshipType, moodType, Set.of(), Set.of());

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
        @DisplayName("도수가 MODERATE_ABV_THRESHOLD를 초과하는 Drink는 맛이 더 가까워도 이하인 Drink보다 점수가 낮다")
        void shouldScoreLowerWhenAbvExceedsModerateThreshold() {
            // given
            // 위스키는 후라이드치킨과 맛이 소주보다 더 가깝지만 고도수 페널티로 뒤집힌다
            given(drinkRepository.findAll())
                    .willReturn(List.of(DrinkFixture.SOJU.create(), DrinkFixture.WHISKEY.create()));
            given(anjuRepository.findAll()).willReturn(List.of(AnjuFixture.FRIED_CHICKEN.create()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQuery(
                            RelationshipType.FRIEND, MoodType.CASUAL, Set.of(), Set.of());

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
        @DisplayName("attendeeConstraints에 DRIVER가 포함되면 상위에서 밀린 무알코올 Drink도 결과에 포함한다")
        void shouldIncludeNonAlcoholicPairingWhenAttendeeRequiresNonAlcoholicOption() {
            // given
            givenAlcoholicDrinksOutrankingNonAlcoholicDrink();
            RecommendPairingQuery query =
                    new RecommendPairingQuery(
                            RelationshipType.FRIEND,
                            MoodType.CASUAL,
                            Set.of(AttendeeConstraint.DRIVER),
                            Set.of());

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
            RecommendPairingQuery query =
                    new RecommendPairingQuery(
                            RelationshipType.FRIEND, MoodType.CASUAL, Set.of(), Set.of());

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            assertThat(results).extracting(PairingResult::drinkNonAlcoholic).containsOnly(false);
        }

        @Test
        @DisplayName("이미 상위에 무알코올 Drink가 있으면 결과를 그대로 유지한다")
        void shouldKeepResultsWhenNonAlcoholicPairingAlreadyIncluded() {
            // given
            given(drinkRepository.findAll())
                    .willReturn(
                            List.of(DrinkFixture.SOJU.create(), DrinkFixture.FRUIT_JUICE.create()));
            givenSingleAnjuAndCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQuery(
                            RelationshipType.FRIEND,
                            MoodType.CASUAL,
                            Set.of(AttendeeConstraint.PREGNANT),
                            Set.of());

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
            given(drinkRepository.findAll()).willReturn(List.of(DrinkFixture.SOJU.create()));
            givenSingleAnjuAndCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQuery(
                            RelationshipType.FRIEND,
                            MoodType.CASUAL,
                            Set.of(AttendeeConstraint.NON_DRINKER),
                            Set.of());

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
            given(drinkRepository.findAll()).willReturn(List.of(DrinkFixture.SOJU.create()));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.GOLBAENGI_MUCHIM.create(),
                                    AnjuFixture.DRIED_SNACK.create()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQuery(
                            RelationshipType.FRIEND,
                            MoodType.CASUAL,
                            Set.of(),
                            Set.of(AllergyType.PEANUT));

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
            given(drinkRepository.findAll()).willReturn(List.of(DrinkFixture.SOJU.create()));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.FRIED_CHICKEN.create(),
                                    AnjuFixture.FRIED_SHRIMP.create(),
                                    AnjuFixture.BOILED_PORK.create(),
                                    AnjuFixture.FRENCH_FRIES.create(),
                                    AnjuFixture.FRUIT_PLATTER.create()));
            givenCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQuery(
                            RelationshipType.FRIEND,
                            MoodType.CASUAL,
                            Set.of(),
                            Set.of(AllergyType.PORK, AllergyType.SHRIMP));

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

        private void givenStandardCatalog() {
            given(drinkRepository.findAll())
                    .willReturn(
                            List.of(
                                    DrinkFixture.SOJU.create(),
                                    DrinkFixture.BEER.create(),
                                    DrinkFixture.WHISKEY.create(),
                                    DrinkFixture.WINE.create()));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.GOLBAENGI_MUCHIM.create(),
                                    AnjuFixture.FRIED_CHICKEN.create(),
                                    AnjuFixture.DRIED_SNACK.create(),
                                    AnjuFixture.TOFU_KIMCHI.create(),
                                    AnjuFixture.CHEESE_PLATTER.create(),
                                    AnjuFixture.FRUIT_PLATTER.create()));
            given(musicMoodRepository.findAll())
                    .willReturn(
                            List.of(
                                    MusicMoodFixture.FORMAL_JAZZ.create(),
                                    MusicMoodFixture.CASUAL_ACOUSTIC.create(),
                                    MusicMoodFixture.CELEBRATORY_DANCE.create()));
        }

        private void givenAlcoholicDrinksOutrankingNonAlcoholicDrink() {
            given(drinkRepository.findAll())
                    .willReturn(List.of(DrinkFixture.SOJU.create(), DrinkFixture.SODA.create()));
            given(anjuRepository.findAll()).willReturn(List.of(AnjuFixture.DRIED_SNACK.create()));
            given(musicMoodRepository.findAll())
                    .willReturn(Collections.nCopies(5, MusicMoodFixture.CASUAL_ACOUSTIC.create()));
        }

        private void givenSingleAnjuAndCasualAcousticMusicMood() {
            given(anjuRepository.findAll()).willReturn(List.of(AnjuFixture.TOFU_KIMCHI.create()));
            givenCasualAcousticMusicMood();
        }

        private void givenCasualAcousticMusicMood() {
            given(musicMoodRepository.findAll())
                    .willReturn(List.of(MusicMoodFixture.CASUAL_ACOUSTIC.create()));
        }
    }
}
