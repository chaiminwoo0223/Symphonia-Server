package com.symphonia.pairing.domain.vo;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Pairing {
    private static final double FLAVOR_WEIGHT = 0.6;
    private static final double LIGHT_ANJU_PENALTY = 0.1;
    private static final double HIGH_ABV_THRESHOLD = 20.0;
    private static final double HIGH_ABV_PENALTY = 0.15;
    private static final double HARMONIZE_BONUS = 0.1;
    private static final double RICHNESS_CUTTING_BONUS = 0.2;
    private static final double CARBONATION_ACIDITY_CUTTING_BONUS = 0.2;
    private static final double MOOD_DRINK_BONUS = 0.05;
    private static final double MOOD_ANJU_BONUS = 0.05;
    private static final double FEEDBACK_WEIGHT = 0.1;
    private static final int RECOMMENDATION_LIMIT = 5;
    private static final double TIE_TOLERANCE = 1e-9;
    private static final Comparator<Combination> RANKING =
            Comparator.comparingDouble(Combination::rawScore)
                    .reversed()
                    .thenComparing(combination -> combination.drink().getId())
                    .thenComparing(combination -> combination.anju().getId());

    private final PairingDrink drink;
    private final PairingAnju anju;
    private final double score;
    private final List<PairingReason> reasons;

    // 선정 규칙에 들 수 있는 음료×안주 쌍만 조합으로 만들고, 피드백이 있는 조합은 경계 밖이어도 함께 만든다.
    // 감점받은 조합이 밀려나거나 제외한 조합이 빠진 자리를 채울 수 있도록 상위 경계는 그 조합 수만큼 넓힌다.
    // 스타일별 최고와 무알코올 최고는 그 경계에 속한 음료의 감점 조합과 제외 조합 수만큼만 넓힌다.
    // 제외 조합은 회원이 싫어요를 남긴 조합이고, 순위를 매긴 뒤 결과에서 뺀다(#175).
    public static List<Pairing> recommend(
            List<PairingDrink> drinks,
            List<PairingAnju> anjus,
            List<PairingRatingSummary> ratingSummaries,
            Set<CombinationId> excluded,
            Occasion occasion) {
        List<PairingDrink> allowedDrinks =
                drinks.stream().filter(drink -> drink.getAbv() <= occasion.maxAbv()).toList();
        List<PairingAnju> allowedAnjus =
                anjus.stream().filter(anju -> !occasion.hasAllergyConflict(anju)).toList();
        Map<Long, PairingDrink> drinkById = byId(allowedDrinks, PairingDrink::getId);
        Map<Long, PairingAnju> anjuById = byId(allowedAnjus, PairingAnju::getId);
        FeedbackScores feedbackScores =
                FeedbackScores.of(
                        ratingSummaries,
                        drinkById.keySet(),
                        anjuById.keySet(),
                        Pairing::feedbackScore);

        List<Combination> combined =
                selectableDrinkAnjus(
                                allowedDrinks, allowedAnjus, occasion, feedbackScores, excluded)
                        .stream()
                        .map(
                                drinkAnju ->
                                        Combination.of(
                                                drinkAnju.drink(),
                                                drinkAnju.anju(),
                                                drinkAnju.score(),
                                                feedbackScores))
                        .toList();
        List<Combination> ranked =
                Stream.concat(
                                combined.stream(),
                                ratedCombinationsOutside(
                                        combined, feedbackScores, drinkById, anjuById, occasion))
                        .filter(combination -> !excluded.contains(combination.id()))
                        .sorted(RANKING)
                        .toList();

        return withNonAlcoholicOption(diversifiedTop(ranked), ranked, occasion).stream()
                .sorted(RANKING)
                .map(combination -> combination.toPairing(occasion))
                .toList();
    }

    private static <T> Map<Long, T> byId(Collection<T> items, Function<T, Long> id) {
        return items.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    private static double feedbackScore(PairingRatingSummary ratingSummary) {
        return FEEDBACK_WEIGHT * (2 * ratingSummary.smoothedLikeRatio() - 1);
    }

    private static List<ScoredDrinkAnju> selectableDrinkAnjus(
            List<PairingDrink> allowedDrinks,
            List<PairingAnju> allowedAnjus,
            Occasion occasion,
            FeedbackScores feedbackScores,
            Set<CombinationId> excluded) {
        DrinkAnjuScores scores =
                DrinkAnjuScores.of(
                        allowedDrinks,
                        allowedAnjus,
                        (drink, anju) -> pairScore(drink, anju, occasion),
                        drink -> drinkMoodBonus(drink, occasion),
                        anju -> anjuMoodBonus(anju, occasion));
        SelectionThresholds thresholds =
                SelectionThresholds.of(
                        scores,
                        RECOMMENDATION_LIMIT + feedbackScores.displacedCount(excluded),
                        feedbackScores.displacedCountByDrink(excluded),
                        occasion.requiresNonAlcoholicOption(),
                        TIE_TOLERANCE);

        return IntStream.range(0, scores.drinkCount())
                .boxed()
                .flatMap(thresholds::selectablePairsOf)
                .toList();
    }

    private static double pairScore(PairingDrink drink, PairingAnju anju, Occasion occasion) {
        double flavorSimilarity = drink.flavorSimilarityTo(anju);

        return FLAVOR_WEIGHT * flavorSimilarity
                + harmonizeBonus(drink, anju)
                + richnessCuttingBonus(drink, anju)
                - lightAnjuPenalty(anju, occasion)
                - highAbvPenalty(drink);
    }

    private static double harmonizeBonus(PairingDrink drink, PairingAnju anju) {
        return drink.harmonizesWith(anju) ? HARMONIZE_BONUS : 0;
    }

    // 도수 대비와 탄산, 산미 대비는 PairingDrink에서 서로 배타적이라 둘 중 하나만 더해진다.
    private static double richnessCuttingBonus(PairingDrink drink, PairingAnju anju) {
        return (abvCuttingBonus(drink, anju) + carbonationAcidityCuttingBonus(drink, anju))
                * anju.getFlavorProfile().heaviness();
    }

    private static double abvCuttingBonus(PairingDrink drink, PairingAnju anju) {
        return drink.cutsRichnessOf(anju) ? RICHNESS_CUTTING_BONUS : 0;
    }

    private static double carbonationAcidityCuttingBonus(PairingDrink drink, PairingAnju anju) {
        return drink.cutsRichnessWithCarbonationOrAcidityOf(anju)
                ? CARBONATION_ACIDITY_CUTTING_BONUS
                        * drink.getFlavorProfile().richnessCuttingStrength()
                : 0;
    }

    private static double lightAnjuPenalty(PairingAnju anju, Occasion occasion) {
        boolean heavyAnjuDespitePreference =
                occasion.prefersLightAnju() && !anju.getFlavorProfile().isLight();

        return heavyAnjuDespitePreference ? LIGHT_ANJU_PENALTY : 0;
    }

    private static double highAbvPenalty(PairingDrink drink) {
        return drink.getAbv() > HIGH_ABV_THRESHOLD ? HIGH_ABV_PENALTY : 0;
    }

    // 분위기는 후보를 거르지 않고, 분위기에 어울리는 음료와 안주에 각각 가산점만 준다.
    private static double drinkMoodBonus(PairingDrink drink, Occasion occasion) {
        return occasion.moodPrefers(drink) ? MOOD_DRINK_BONUS : 0;
    }

    private static double anjuMoodBonus(PairingAnju anju, Occasion occasion) {
        return occasion.moodPrefers(anju) ? MOOD_ANJU_BONUS : 0;
    }

    private static Stream<Combination> ratedCombinationsOutside(
            List<Combination> combinations,
            FeedbackScores feedbackScores,
            Map<Long, PairingDrink> drinkById,
            Map<Long, PairingAnju> anjuById,
            Occasion occasion) {
        Set<CombinationId> combined =
                combinations.stream().map(Combination::id).collect(Collectors.toSet());

        return feedbackScores.combinations().stream()
                .filter(id -> !combined.contains(id))
                .map(
                        id -> {
                            PairingDrink drink = drinkById.get(id.drinkId());
                            PairingAnju anju = anjuById.get(id.anjuId());
                            return Combination.of(
                                    drink,
                                    anju,
                                    drinkAnjuScore(drink, anju, occasion),
                                    feedbackScores);
                        });
    }

    // DrinkAnjuScores와 같은 순서로 더해, 경계 밖 조합의 점수도 배열에 담긴 점수와 비트 단위로 같게 한다.
    private static double drinkAnjuScore(PairingDrink drink, PairingAnju anju, Occasion occasion) {
        return pairScore(drink, anju, occasion)
                + drinkMoodBonus(drink, occasion)
                + anjuMoodBonus(anju, occasion);
    }

    // 스타일마다 최고점 조합을 하나씩 먼저 고르고, 스타일이 모자라면 남은 조합을 점수순으로 채운다.
    private static List<Combination> diversifiedTop(List<Combination> ranked) {
        List<Combination> bestPerStyle =
                ranked.stream()
                        .collect(
                                Collectors.toMap(
                                        combination -> combination.drink().getDrinkStyleId(),
                                        Function.identity(),
                                        (best, _) -> best,
                                        LinkedHashMap::new))
                        .values()
                        .stream()
                        .limit(RECOMMENDATION_LIMIT)
                        .toList();

        return Stream.concat(
                        bestPerStyle.stream(),
                        ranked.stream().filter(combination -> !bestPerStyle.contains(combination)))
                .limit(RECOMMENDATION_LIMIT)
                .toList();
    }

    private static List<Combination> withNonAlcoholicOption(
            List<Combination> top, List<Combination> ranked, Occasion occasion) {
        return Optional.of(top)
                .filter(_ -> occasion.requiresNonAlcoholicOption())
                .filter(picked -> picked.stream().noneMatch(Combination::isNonAlcoholic))
                .flatMap(_ -> ranked.stream().filter(Combination::isNonAlcoholic).findFirst())
                .map(bestNonAlcoholic -> replaceLast(top, bestNonAlcoholic))
                .orElse(top);
    }

    private static List<Combination> replaceLast(List<Combination> top, Combination replacement) {
        return Stream.concat(top.stream().limit(top.size() - 1L), Stream.of(replacement)).toList();
    }

    private record Combination(PairingDrink drink, PairingAnju anju, double rawScore) {
        private static Combination of(
                PairingDrink drink,
                PairingAnju anju,
                double drinkAnjuScore,
                FeedbackScores feedbackScores) {
            return new Combination(
                    drink,
                    anju,
                    drinkAnjuScore
                            + feedbackScores.scoreOf(
                                    new CombinationId(drink.getId(), anju.getId())));
        }

        private CombinationId id() {
            return new CombinationId(drink.getId(), anju.getId());
        }

        private boolean isNonAlcoholic() {
            return drink.isNonAlcoholic();
        }

        private Pairing toPairing(Occasion occasion) {
            PairingReasonContext context = PairingReasonContext.of(drink, anju, occasion);

            return new Pairing(
                    drink, anju, Math.max(0, rawScore), PairingReason.allApplicableTo(context));
        }
    }
}
