package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.MusicMood;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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
                        occasion);
        CandidateThresholds thresholds =
                CandidateThresholds.of(scores, occasion.requiresNonAlcoholicOption());

        return IntStream.range(0, scores.drinkCount())
                .boxed()
                .flatMap(thresholds::candidatesOf)
                .toList();
    }

    private static double drinkAnjuScore(Drink drink, Anju anju, Occasion occasion) {
        double flavorSimilarity = drink.flavorSimilarityTo(anju);

        return FLAVOR_WEIGHT * flavorSimilarity
                - lightAnjuPenalty(anju, occasion)
                - highAbvPenalty(drink);
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

    private record ScoredDrinkAnju(Drink drink, Anju anju, double score) {}

    // 허용된 음료×안주 쌍의 점수를 1차원 배열로 들고 있다.
    // index번째 쌍은 (index / 안주 수)번째 음료와 (index % 안주 수)번째 안주라서, 한 음료의 쌍은 배열에서 연속한다.
    private static final class DrinkAnjuScores {
        private final List<Drink> drinks;
        private final List<Anju> anjus;
        private final double[] scores;

        private DrinkAnjuScores(List<Drink> drinks, List<Anju> anjus, double[] scores) {
            this.drinks = drinks;
            this.anjus = anjus;
            this.scores = scores;
        }

        // 요청마다 쌍 수만큼 도는 가장 뜨거운 반복이라 이중 반복문으로 둔다.
        // flatMapToDouble로 바꾸면 음료마다 안쪽 스트림이 생겨 추천 계산 시간이 약 2배가 된다(#107 측정).
        private static DrinkAnjuScores of(List<Drink> drinks, List<Anju> anjus, Occasion occasion) {
            double[] scores = new double[drinks.size() * anjus.size()];
            int index = 0;
            for (Drink drink : drinks) {
                for (Anju anju : anjus) {
                    scores[index++] = drinkAnjuScore(drink, anju, occasion);
                }
            }

            return new DrinkAnjuScores(drinks, anjus, scores);
        }

        private List<Drink> drinks() {
            return drinks;
        }

        private int drinkCount() {
            return drinks.size();
        }

        private double[] allScores() {
            return scores;
        }

        // 한 음료와 허용된 안주들의 쌍 중 가장 높은 점수다. 안주가 없으면 음의 무한대다.
        private double bestOf(int drinkIndex) {
            int from = drinkIndex * anjus.size();

            return Arrays.stream(scores, from, from + anjus.size())
                    .max()
                    .orElse(Double.NEGATIVE_INFINITY);
        }

        // 한 음료의 쌍 중 점수가 경계 이상인 쌍을 안주 순서대로 만든다.
        private Stream<ScoredDrinkAnju> pairsOf(int drinkIndex, double threshold) {
            Drink drink = drinks.get(drinkIndex);
            int from = drinkIndex * anjus.size();

            return IntStream.range(0, anjus.size())
                    .filter(anjuIndex -> scores[from + anjuIndex] >= threshold)
                    .mapToObj(
                            anjuIndex ->
                                    new ScoredDrinkAnju(
                                            drink, anjus.get(anjuIndex), scores[from + anjuIndex]));
        }
    }

    // 쌍이 후보에 드는 경계 점수를 음료마다 하나씩 들고 있다.
    // 세 선정 규칙의 경계는 nearTop과 같다. 상위 5개는 5번째로 높은 점수이고, 스타일별 최고와 무알코올 최고는 최고 점수다.
    // 쌍은 경계 중 하나라도 넘으면 후보이므로, 음료의 경계는 그 음료에 해당하는 경계 중 가장 낮은 값이다.
    // 각 경계와 TIE_TOLERANCE 안에 있는 쌍도 남긴다.
    private static final class CandidateThresholds {
        private final DrinkAnjuScores scores;
        private final double[] thresholdByDrink;

        private CandidateThresholds(DrinkAnjuScores scores, double[] thresholdByDrink) {
            this.scores = scores;
            this.thresholdByDrink = thresholdByDrink;
        }

        private static CandidateThresholds of(DrinkAnjuScores scores, boolean nonAlcoholicOption) {
            List<Drink> drinks = scores.drinks();
            double top = topThreshold(scores);
            double[] bestByDrink =
                    IntStream.range(0, drinks.size()).mapToDouble(scores::bestOf).toArray();
            Map<Long, Double> bestByStyle =
                    IntStream.range(0, drinks.size())
                            .boxed()
                            .collect(
                                    Collectors.toMap(
                                            index -> drinks.get(index).getDrinkStyleId(),
                                            index -> bestByDrink[index],
                                            Math::max));
            double bestNonAlcoholic =
                    IntStream.range(0, drinks.size())
                            .filter(index -> drinks.get(index).isNonAlcoholic())
                            .mapToDouble(index -> bestByDrink[index])
                            .max()
                            .orElse(Double.NEGATIVE_INFINITY);

            double[] thresholdByDrink =
                    drinks.stream()
                            .mapToDouble(
                                    drink ->
                                            DoubleStream.of(
                                                            top,
                                                            bestByStyle.get(
                                                                    drink.getDrinkStyleId()),
                                                            nonAlcoholicOption
                                                                            && drink
                                                                                    .isNonAlcoholic()
                                                                    ? bestNonAlcoholic
                                                                    : Double.POSITIVE_INFINITY)
                                                    .min()
                                                    .orElseThrow())
                            .toArray();

            return new CandidateThresholds(scores, thresholdByDrink);
        }

        // 상위 5개 경계는 정렬하지 않고 점수를 한 번 훑으며 구한다.
        // TopScores가 누적하는 상태를 스트림 forEach로 바꾸면 부작용이 숨으므로 반복문으로 둔다.
        private static double topThreshold(DrinkAnjuScores scores) {
            TopScores top = new TopScores(RECOMMENDATION_LIMIT);
            for (double score : scores.allScores()) {
                top.offer(score);
            }

            return top.threshold();
        }

        private Stream<ScoredDrinkAnju> candidatesOf(int drinkIndex) {
            return scores.pairsOf(drinkIndex, thresholdByDrink[drinkIndex] - TIE_TOLERANCE);
        }
    }

    // 점수를 정렬하지 않고 상위 limit개만 내림차순으로 들고 있다가 limit번째 점수를 경계로 돌려준다.
    // 같은 점수도 하나씩 세므로 정렬 후 limit번째를 고르는 nearTop과 경계가 같다.
    // 점수가 limit개 이하면 모두 남겨야 하므로 경계는 음의 무한대다.
    private static final class TopScores {
        private final double[] scores;
        private int size;

        private TopScores(int limit) {
            this.scores = new double[limit];
        }

        private void offer(double score) {
            if (size == scores.length && score <= scores[size - 1]) {
                return;
            }
            int position = size == scores.length ? size - 1 : size++;
            while (position > 0 && scores[position - 1] < score) {
                scores[position] = scores[position - 1];
                position--;
            }
            scores[position] = score;
        }

        private double threshold() {
            return size == scores.length ? scores[size - 1] : Double.NEGATIVE_INFINITY;
        }
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
