package com.symphonia.pairing.domain.vo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// 쌍이 선정 규칙(상위 topLimit개, 스타일별 최고, 무알코올 최고)에 들 수 있는 경계 점수를 음료마다 하나씩 들고 있다.
// 스타일별 최고와 무알코올 최고는 기본으로 최고 점수가 경계이고, 그 경계에 속한 음료의 감점 조합과 제외 조합 수만큼 순위를 넓힌다(#130, #175).
// 음료의 경계는 그 음료에 해당하는 경계 중 가장 낮은 값에서 tolerance를 뺀 값이다.
public record SelectionThresholds(DrinkAnjuScores scores, double[] thresholdByDrink) {
    public static SelectionThresholds of(
            DrinkAnjuScores scores,
            int topLimit,
            Map<Long, Integer> displacedCountByDrink,
            boolean nonAlcoholicOption,
            double tolerance) {
        double top = rankedScore(scores, IntStream.range(0, scores.drinkCount()), topLimit);
        Map<Long, Double> bestByStyle = bestByStyle(scores, displacedCountByDrink);
        double bestNonAlcoholic =
                nonAlcoholicOption
                        ? bestNonAlcoholic(scores, displacedCountByDrink)
                        : Double.POSITIVE_INFINITY;

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

    private static Map<Long, Double> bestByStyle(
            DrinkAnjuScores scores, Map<Long, Integer> displacedCountByDrink) {
        List<PairingDrink> drinks = scores.drinks();

        return IntStream.range(0, drinks.size())
                .boxed()
                .collect(
                        Collectors.groupingBy(
                                index -> drinks.get(index).getDrinkStyleId(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        indexes ->
                                                bestRankedScore(
                                                        scores,
                                                        indexes.stream()
                                                                .mapToInt(Integer::intValue),
                                                        displacedCountByDrink))));
    }

    private static double bestNonAlcoholic(
            DrinkAnjuScores scores, Map<Long, Integer> displacedCountByDrink) {
        List<PairingDrink> drinks = scores.drinks();
        IntStream nonAlcoholicIndexes =
                IntStream.range(0, drinks.size())
                        .filter(index -> drinks.get(index).isNonAlcoholic());

        return bestRankedScore(scores, nonAlcoholicIndexes, displacedCountByDrink);
    }

    // 최고 자리는 같은 경계에 속한 음료의 감점 조합과 제외 조합만 밀어낼 수 있어, 그 수만큼만 순위를 넓힌다.
    private static double bestRankedScore(
            DrinkAnjuScores scores,
            IntStream drinkIndexes,
            Map<Long, Integer> displacedCountByDrink) {
        int[] indexes = drinkIndexes.toArray();
        int rank =
                1
                        + Arrays.stream(indexes)
                                .map(
                                        index ->
                                                displacedCountByDrink.getOrDefault(
                                                        scores.drinks().get(index).getId(), 0))
                                .sum();

        return rankedScore(scores, Arrays.stream(indexes), rank);
    }

    // 정렬하지 않고 점수를 한 번 훑으며 rank번째로 높은 점수를 구한다. 점수가 rank개보다 적으면 음의 무한대다.
    // 요청마다 모든 쌍을 훑는 반복이라 쌍 단위로는 스트림을 쓰지 않는다(#130 측정).
    private static double rankedScore(DrinkAnjuScores scores, IntStream drinkIndexes, int rank) {
        TopScores top = TopScores.of(rank);
        drinkIndexes.forEach(index -> scores.forEachScoreOf(index, top::offer));

        return top.threshold();
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
    // 대부분의 점수는 맨 앞 점수 이하라 비교 한 번으로 버리고, 그보다 높은 드문 점수만 이진 탐색으로 자리를 찾는다.
    private record TopScores(double[] scores) {
        private static TopScores of(int limit) {
            double[] scores = new double[limit];
            Arrays.fill(scores, Double.NEGATIVE_INFINITY);

            return new TopScores(scores);
        }

        // 자리보다 낮은 점수를 한 칸씩 앞으로 밀어 맨 앞 점수를 버리고, 비운 자리에 점수를 넣는다.
        // 모든 쌍의 점수마다 불리므로 대부분을 비교 한 번으로 버리는 조건문을 둔다(#130 측정).
        private void offer(double score) {
            if (score <= threshold()) {
                return;
            }
            int slot = insertionPointOf(score) - 1;
            System.arraycopy(scores, 1, scores, 0, slot);
            scores[slot] = score;
        }

        private int insertionPointOf(double score) {
            int found = Arrays.binarySearch(scores, score);

            return found >= 0 ? found : -found - 1;
        }

        private double threshold() {
            return scores[0];
        }
    }
}
