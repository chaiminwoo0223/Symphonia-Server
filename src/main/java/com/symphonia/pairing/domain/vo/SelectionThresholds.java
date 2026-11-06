package com.symphonia.pairing.domain.vo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// 쌍이 선정 규칙(상위 topLimit개, 스타일별 최고, 무알코올 최고)에 들 수 있는 경계 점수를 음료마다 하나씩 들고 있다.
// 음료의 경계는 그 음료에 해당하는 경계 중 가장 낮은 값에서 tolerance를 뺀 값이다.
public record SelectionThresholds(DrinkAnjuScores scores, double[] thresholdByDrink) {
    public static SelectionThresholds of(
            DrinkAnjuScores scores, int topLimit, boolean nonAlcoholicOption, double tolerance) {
        List<PairingDrink> drinks = scores.drinks();
        double top = rankedScore(Arrays.stream(scores.scores()), topLimit);
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
                nonAlcoholicOption
                        ? IntStream.range(0, drinks.size())
                                .filter(index -> drinks.get(index).isNonAlcoholic())
                                .mapToDouble(index -> bestByDrink[index])
                                .max()
                                .orElse(Double.NEGATIVE_INFINITY)
                        : Double.POSITIVE_INFINITY;

        double[] thresholdByDrink =
                drinks.stream()
                        .mapToDouble(
                                drink ->
                                        thresholdOf(drink, top, bestByStyle, bestNonAlcoholic)
                                                - tolerance)
                        .toArray();

        return new SelectionThresholds(scores, thresholdByDrink);
    }

    public Stream<ScoredDrinkAnju> selectablePairsOf(int drinkIndex) {
        return scores.pairsOf(drinkIndex, thresholdByDrink[drinkIndex]);
    }

    // 정렬하지 않고 점수를 한 번 훑으며 rank번째로 높은 점수를 구한다. 점수가 rank개보다 적으면 음의 무한대다.
    private static double rankedScore(DoubleStream scores, int rank) {
        return scores.collect(() -> TopScores.of(rank), TopScores::offer, TopScores::offerAll)
                .threshold();
    }

    private static double thresholdOf(
            PairingDrink drink,
            double top,
            Map<Long, Double> bestByStyle,
            double bestNonAlcoholic) {
        double nonAlcoholic = drink.isNonAlcoholic() ? bestNonAlcoholic : Double.POSITIVE_INFINITY;

        return DoubleStream.of(top, bestByStyle.get(drink.getDrinkStyleId()), nonAlcoholic)
                .min()
                .orElseThrow();
    }

    // 상위 limit개를 오름차순으로 들고 있어 맨 앞 점수가 limit번째로 높은 점수다.
    // 맨 앞 점수 이하인 점수는 자리가 0이라 옮기는 점수가 없고, max로 버려진다.
    private record TopScores(double[] scores) {
        private static TopScores of(int limit) {
            double[] scores = new double[limit];
            Arrays.fill(scores, Double.NEGATIVE_INFINITY);

            return new TopScores(scores);
        }

        private void offer(double score) {
            int slot = Math.max(insertionPointOf(score) - 1, 0);
            System.arraycopy(scores, 1, scores, 0, slot);
            scores[slot] = Math.max(scores[slot], score);
        }

        private int insertionPointOf(double score) {
            int found = Arrays.binarySearch(scores, score);

            return found >= 0 ? found : -found - 1;
        }

        private void offerAll(TopScores other) {
            Arrays.stream(other.scores).forEach(this::offer);
        }

        private double threshold() {
            return scores[0];
        }
    }
}
