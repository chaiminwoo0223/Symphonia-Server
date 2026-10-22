package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.MusicMood;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Pairing {
    private static final double FLAVOR_WEIGHT = 0.6;
    private static final double MOOD_WEIGHT = 0.4;
    private static final double LIGHT_ANJU_PENALTY = 0.1;
    private static final double HIGH_ABV_THRESHOLD = 20.0;
    private static final double HIGH_ABV_PENALTY = 0.15;
    // 음식 계열이 맞으면 가벼운 안주 감점과 같은 크기만큼 더한다. 맛 유사도로 약 0.17 차이를 뒤집는 크기다.
    private static final double HARMONIZE_BONUS = 0.1;
    // 무게감 차이로 깎이는 맛 유사도 점수보다 커야 도수 높은 술이 기름진 안주를 가벼운 안주보다 앞에 둔다.
    // 그 점수는 소주와 삼겹살구이를, 삼겹살구이의 무게감만 1로 바꾼 경우와 비교하면 약 0.13이다.
    private static final double RICHNESS_CUTTING_BONUS = 0.2;
    // 도수 대비와 같은 크기로 둬서, 탄산 5인 맥주가 기름진 안주에서 소주와 같은 대비 점수를 받는다.
    private static final double CARBONATION_ACIDITY_CUTTING_BONUS = 0.2;
    private static final int RECOMMENDATION_LIMIT = 5;
    // 서로 다른 점수에 같은 값을 더하면 반올림으로 동점이 될 수 있다. 경계와 이 오차 안에 있는 후보는 모두 남긴다.
    private static final double TIE_TOLERANCE = 1e-9;
    private static final Comparator<Combination> RANKING =
            Comparator.comparingDouble(Combination::rawScore)
                    .reversed()
                    .thenComparing(combination -> combination.drink().getId())
                    .thenComparing(combination -> combination.anju().getId())
                    .thenComparing(combination -> combination.musicMood().getId());

    private final Drink drink;
    private final Anju anju;
    private final MusicMood musicMood;
    private final double score;
    private final List<PairingReason> reasons;

    // 조합 점수는 음료×안주 점수와 음악 점수의 합이다. 그래서 전체 조합을 만들지 않고
    // 각 선정 규칙(상위 5개, 스타일별 최고, 무알코올 최고)에 들 수 있는 음료×안주 쌍과 상위 음악만 결합한다.
    public static List<Pairing> recommend(
            List<Drink> drinks, List<Anju> anjus, List<MusicMood> musicMoods, Occasion occasion) {
        List<ScoredDrinkAnju> drinkAnjus = candidateDrinkAnjus(drinks, anjus, occasion);
        List<ScoredMusicMood> topMusicMoods =
                nearTop(
                        scoreMusicMoods(musicMoods, occasion),
                        ScoredMusicMood::score,
                        RECOMMENDATION_LIMIT);

        List<Combination> ranked =
                combine(drinkAnjus, topMusicMoods).stream().sorted(RANKING).toList();
        List<Combination> top = diversifiedTop(ranked);
        List<Combination> recommended =
                occasion.requiresNonAlcoholicOption() ? withNonAlcoholicOption(top, ranked) : top;

        return recommended.stream()
                .sorted(RANKING)
                .map(combination -> combination.toPairing(occasion))
                .toList();
    }

    // 음료×안주 쌍은 카탈로그 크기의 곱이라 요청마다 수십만 개가 된다(#107).
    // 그래서 점수는 배열에만 담고, 세 선정 규칙의 경계를 구한 뒤 경계를 넘는 쌍만 객체로 만든다.
    private static List<ScoredDrinkAnju> candidateDrinkAnjus(
            List<Drink> drinks, List<Anju> anjus, Occasion occasion) {
        DrinkAnjuScores scores =
                DrinkAnjuScores.of(
                        drinks.stream()
                                .filter(drink -> drink.getAbv() <= occasion.maxAbv())
                                .toList(),
                        anjus.stream().filter(anju -> !occasion.hasAllergyConflict(anju)).toList(),
                        (drink, anju) -> drinkAnjuScore(drink, anju, occasion));
        CandidateThresholds thresholds =
                CandidateThresholds.of(
                        scores,
                        RECOMMENDATION_LIMIT,
                        occasion.requiresNonAlcoholicOption(),
                        TIE_TOLERANCE);

        return IntStream.range(0, scores.drinkCount())
                .boxed()
                .flatMap(thresholds::candidatesOf)
                .toList();
    }

    private static double drinkAnjuScore(Drink drink, Anju anju, Occasion occasion) {
        double flavorSimilarity = drink.flavorSimilarityTo(anju);

        return FLAVOR_WEIGHT * flavorSimilarity
                + harmonizeBonus(drink, anju)
                + richnessCuttingBonus(drink, anju)
                - lightAnjuPenalty(anju, occasion)
                - highAbvPenalty(drink);
    }

    private static double harmonizeBonus(Drink drink, Anju anju) {
        return drink.harmonizesWith(anju) ? HARMONIZE_BONUS : 0;
    }

    // 안주가 무거울수록 씻어 줄 기름기가 많아 무게감 5에서 최대가 된다.
    // 도수와 탄산, 산미는 같은 기름기를 씻는 효과라 둘 중 하나만 준다.
    // 도수 대비가 탄산, 산미 대비의 최대치와 같아서, 둘 다 해당하는 술은 도수 대비를 받는다.
    private static double richnessCuttingBonus(Drink drink, Anju anju) {
        double heaviness = anju.getFlavorProfile().heaviness();
        if (drink.cutsRichnessOf(anju)) {
            return RICHNESS_CUTTING_BONUS * heaviness;
        }
        if (drink.cutsRichnessWithCarbonationOrAcidityOf(anju)) {
            return CARBONATION_ACIDITY_CUTTING_BONUS
                    * drink.getFlavorProfile().richnessCuttingStrength()
                    * heaviness;
        }
        return 0;
    }

    private static double lightAnjuPenalty(Anju anju, Occasion occasion) {
        boolean heavyAnjuDespitePreference =
                occasion.prefersLightAnju() && !anju.getFlavorProfile().isLight();

        return heavyAnjuDespitePreference ? LIGHT_ANJU_PENALTY : 0;
    }

    private static double highAbvPenalty(Drink drink) {
        return drink.getAbv() > HIGH_ABV_THRESHOLD ? HIGH_ABV_PENALTY : 0;
    }

    // 점수가 limit번째로 높은 값 이상인 항목을 모두 고른다. 경계와 동점이거나 오차 안에 있는 항목도 남긴다.
    private static <T> List<T> nearTop(Collection<T> items, ToDoubleFunction<T> score, int limit) {
        if (items.size() <= limit) {
            return List.copyOf(items);
        }

        double threshold =
                items.stream()
                        .map(score::applyAsDouble)
                        .sorted(Comparator.reverseOrder())
                        .skip(limit - 1L)
                        .findFirst()
                        .orElseThrow();

        return items.stream()
                .filter(item -> score.applyAsDouble(item) >= threshold - TIE_TOLERANCE)
                .toList();
    }

    private static List<ScoredMusicMood> scoreMusicMoods(
            List<MusicMood> musicMoods, Occasion occasion) {
        MoodProfile occasionMoodProfile = occasion.toMoodProfile();

        List<ScoredMusicMood> scoredMusicMoods = new ArrayList<>();
        for (MusicMood musicMood : musicMoods) {
            double moodFitness = musicMood.fitnessFor(occasionMoodProfile);
            scoredMusicMoods.add(new ScoredMusicMood(musicMood, MOOD_WEIGHT * moodFitness));
        }

        return scoredMusicMoods;
    }

    private static List<Combination> combine(
            List<ScoredDrinkAnju> drinkAnjus, List<ScoredMusicMood> musicMoods) {
        List<Combination> combinations = new ArrayList<>();
        for (ScoredDrinkAnju drinkAnju : drinkAnjus) {
            for (ScoredMusicMood musicMood : musicMoods) {
                combinations.add(
                        new Combination(
                                drinkAnju.drink(),
                                drinkAnju.anju(),
                                musicMood.musicMood(),
                                drinkAnju.score() + musicMood.score()));
            }
        }

        return combinations;
    }

    // 스타일마다 최고점 조합을 하나씩 먼저 고르고, 스타일이 모자라면 남은 조합을 점수순으로 채운다.
    private static List<Combination> diversifiedTop(List<Combination> ranked) {
        List<Combination> picked = new ArrayList<>();
        Set<Long> pickedDrinkStyleIds = new HashSet<>();
        for (Combination combination : ranked) {
            if (picked.size() == RECOMMENDATION_LIMIT) {
                break;
            }
            if (pickedDrinkStyleIds.add(combination.drink().getDrinkStyleId())) {
                picked.add(combination);
            }
        }

        List<Combination> leftovers =
                ranked.stream()
                        .filter(combination -> !picked.contains(combination))
                        .limit(RECOMMENDATION_LIMIT - picked.size())
                        .toList();
        picked.addAll(leftovers);

        return picked;
    }

    private static List<Combination> withNonAlcoholicOption(
            List<Combination> top, List<Combination> ranked) {
        if (top.stream().anyMatch(combination -> combination.drink().isNonAlcoholic())) {
            return top;
        }

        return ranked.stream()
                .filter(combination -> combination.drink().isNonAlcoholic())
                .findFirst()
                .map(bestNonAlcoholic -> replaceLast(top, bestNonAlcoholic))
                .orElse(top);
    }

    private static List<Combination> replaceLast(List<Combination> top, Combination replacement) {
        List<Combination> replaced = new ArrayList<>(top.subList(0, top.size() - 1));
        replaced.add(replacement);
        return replaced;
    }

    private record ScoredMusicMood(MusicMood musicMood, double score) {}

    // 순위를 매기는 동안 쓰는 후보 조합이다. 추천 사유는 최종으로 고른 조합에만 계산한다.
    // 순위는 감점이 반영된 원점수로 매긴다. 0으로 뭉개면 나쁜 조합끼리의 순서가 id 순으로 바뀐다.
    private record Combination(Drink drink, Anju anju, MusicMood musicMood, double rawScore) {
        // 사용자에게 보여주는 점수만 음수가 되지 않게 0에서 자른다.
        private Pairing toPairing(Occasion occasion) {
            PairingCandidate candidate = PairingCandidate.of(drink, anju, musicMood, occasion);

            return new Pairing(
                    drink,
                    anju,
                    musicMood,
                    Math.max(0, rawScore),
                    PairingReason.allApplicableTo(candidate));
        }
    }
}
