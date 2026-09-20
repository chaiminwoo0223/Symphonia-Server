package com.symphonia.pairing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
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
                    .willReturn(
                            List.of(
                                    DrinkFixture.LOW_ABV_BEER.create(),
                                    DrinkFixture.SOJU.create()));
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
                    .containsOnly(DrinkFixture.LOW_ABV_BEER.getName());
        }

        @Test
        @DisplayName("prefersLightAnju가 true면 richness가 임계값을 초과하는 안주의 점수가 낮아진다")
        void shouldPenalizeHeavyAnjuWhenPrefersLightAnju() {
            // given
            RelationshipType relationshipType = RelationshipType.FRIEND;
            MoodType moodType = MoodType.FORMAL;
            Occasion occasion = Occasion.of(relationshipType, moodType, Set.of(), Set.of());
            MusicMood musicMood = MusicMoodFixture.FORMAL_JAZZ.create(occasion.toMoodProfile());
            given(drinkRepository.findAll()).willReturn(List.of(DrinkFixture.BALANCED.create()));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.LIGHT_BALANCED.create(),
                                    AnjuFixture.HEAVY_BALANCED.create()));
            given(musicMoodRepository.findAll()).willReturn(List.of(musicMood));
            RecommendPairingQuery query =
                    new RecommendPairingQuery(relationshipType, moodType, Set.of(), Set.of());

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            PairingResult lightResult =
                    results.stream()
                            .filter(r -> r.anjuName().equals(AnjuFixture.LIGHT_BALANCED.getName()))
                            .findFirst()
                            .orElseThrow();
            PairingResult heavyResult =
                    results.stream()
                            .filter(r -> r.anjuName().equals(AnjuFixture.HEAVY_BALANCED.getName()))
                            .findFirst()
                            .orElseThrow();

            assertThat(lightResult.score()).isEqualTo(1.0, within(1e-9));
            assertThat(heavyResult.score()).isEqualTo(0.72, within(1e-9));
            assertThat(results.getFirst().anjuName())
                    .isEqualTo(AnjuFixture.LIGHT_BALANCED.getName());
        }

        @Test
        @DisplayName("도수가 MODERATE_ABV_THRESHOLD를 초과하는 Drink는 이하인 Drink보다 점수가 낮다")
        void shouldScoreLowerWhenAbvExceedsModerateThreshold() {
            // given
            given(drinkRepository.findAll())
                    .willReturn(
                            List.of(
                                    DrinkFixture.BALANCED.create(),
                                    DrinkFixture.HIGH_ABV_BALANCED.create()));
            givenSingleLightAnjuAndCasualAcousticMusicMood();
            RecommendPairingQuery query =
                    new RecommendPairingQuery(
                            RelationshipType.FRIEND, MoodType.CASUAL, Set.of(), Set.of());

            // when
            List<PairingResult> results = pairingQueryService.recommend(query);

            // then
            PairingResult moderateAbvResult =
                    results.stream()
                            .filter(r -> r.drinkName().equals(DrinkFixture.BALANCED.getName()))
                            .findFirst()
                            .orElseThrow();
            PairingResult highAbvResult =
                    results.stream()
                            .filter(
                                    r ->
                                            r.drinkName()
                                                    .equals(
                                                            DrinkFixture.HIGH_ABV_BALANCED
                                                                    .getName()))
                            .findFirst()
                            .orElseThrow();

            assertThat(highAbvResult.score()).isLessThan(moderateAbvResult.score());
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
            givenSingleLightAnjuAndCasualAcousticMusicMood();
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
            givenSingleLightAnjuAndCasualAcousticMusicMood();
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
            given(drinkRepository.findAll()).willReturn(List.of(DrinkFixture.BALANCED.create()));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.LIGHT_BALANCED.create(),
                                    AnjuFixture.PEANUT_ALLERGY.create()));
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
                    .containsOnly(AnjuFixture.LIGHT_BALANCED.getName());
        }

        private void givenStandardCatalog() {
            given(drinkRepository.findAll())
                    .willReturn(
                            List.of(
                                    DrinkFixture.SOJU.create(),
                                    DrinkFixture.LOW_ABV_BEER.create(),
                                    DrinkFixture.WHISKEY.create()));
            given(anjuRepository.findAll())
                    .willReturn(
                            List.of(
                                    AnjuFixture.GOLBAENGI_MUCHIM.create(),
                                    AnjuFixture.FRIED_CHICKEN.create(),
                                    AnjuFixture.DRIED_SNACK.create()));
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

        private void givenSingleLightAnjuAndCasualAcousticMusicMood() {
            given(anjuRepository.findAll())
                    .willReturn(List.of(AnjuFixture.LIGHT_BALANCED.create()));
            givenCasualAcousticMusicMood();
        }

        private void givenCasualAcousticMusicMood() {
            given(musicMoodRepository.findAll())
                    .willReturn(List.of(MusicMoodFixture.CASUAL_ACOUSTIC.create()));
        }
    }
}
