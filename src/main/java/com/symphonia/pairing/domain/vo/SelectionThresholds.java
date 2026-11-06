package com.symphonia.pairing.domain.vo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// 쌍이 선정 규칙(상위 topLimit개, 스타일별 최고, 무알코올 최고)에 들 수 있는 경계 점수를 음료마다 하나씩 들고 있다.
// 스타일별 최고와 무알코올 최고는 bestRank번째 점수가 경계다. 피드백 감점이 있으면 Pairing이 두 값을 넓힌다(#130).
// 음료의 경계는 그 음료에 해당하는 경계 중 가장 낮은 값에서 tolerance를 뺀 값이다.
public record SelectionThresholds(DrinkAnjuScores scores, double[] thresholdByDrink) {
    public static SelectionThresholds of(
            DrinkAnjuScores scores,
            int topLimit,
            int bestRank,
            boolean nonAlcoholicOption,
            double tolerance) {
        double top = rankedScore(Arrays.stream(scores.scores()), topLimit);
        Map<Long, Double> bestByStyle = bestByStyle(scores, bestRank);
        double bestNonAlcoholic =
                nonAlcoholicOption ? bestNonAlcoholic(scores, bestRank) : Double.POSITIVE_INFINITY;

        double[] thresholdByDrink =
                scores.drinks().stream()
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

    private static Map<Long, Double> bestByStyle(DrinkAnjuScores scores, int bestRank) {
        List<PairingDrink> drinks = scores.drinks();

        return IntStream.range(0, drinks.size())
                .boxed()
                .collect(
                        Collectors.groupingBy(
                                index -> drinks.get(index).getDrinkStyleId(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        indexes ->
                                                rankedScore(
                                                        pairScoresOf(scores, indexes.stream()),
                                                        bestRank))));
    }

    private static DoubleStream pairScoresOf(DrinkAnjuScores scores, Stream<Integer> drinkIndexes) {
        return drinkIndexes.flatMapToDouble(scores::scoresOf);
    }

    private static double bestNonAlcoholic(DrinkAnjuScores scores, int bestRank) {
        List<PairingDrink> drinks = scores.drinks();
        Stream<Integer> nonAlcoholicIndexes =
                IntStream.range(0, drinks.size())
                        .filter(index -> drinks.get(index).isNonAlcoholic())
                        .boxed();

        return rankedScore(pairScoresOf(scores, nonAlcoholicIndexes), bestRank);
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
