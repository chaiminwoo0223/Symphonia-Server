package com.symphonia.pairing.domain.vo;

import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.pairing.domain.exception.InvalidTopLimitException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// 쌍이 추천 후보에 드는 경계 점수를 음료마다 하나씩 들고 있다.
// 선정 규칙은 상위 topLimit개, 스타일별 최고, 무알코올 최고(무알코올 옵션이 필요할 때)다.
// 상위 topLimit개의 경계는 topLimit번째로 높은 점수이고, 스타일별 최고와 무알코올 최고의 경계는 최고 점수다.
// 쌍은 경계 중 하나라도 넘으면 후보이므로, 음료의 경계는 그 음료에 해당하는 경계 중 가장 낮은 값이다.
// 각 경계와 tolerance 안에 있는 쌍도 남기도록 thresholdByDrink에는 tolerance를 뺀 값을 담는다.
// thresholdByDrink는 DrinkAnjuScores의 scores처럼 비교하지 않고 후보 계산에만 쓰므로 복사하지 않는다.
public record CandidateThresholds(DrinkAnjuScores scores, double[] thresholdByDrink) {
    public static CandidateThresholds of(
            DrinkAnjuScores scores, int topLimit, boolean nonAlcoholicOption, double tolerance) {
        List<Drink> drinks = scores.drinks();
        double top = topThreshold(scores, topLimit);
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
                                                        .orElseThrow()
                                                - tolerance)
                        .toArray();

        return new CandidateThresholds(scores, thresholdByDrink);
    }

    public Stream<ScoredDrinkAnju> candidatesOf(int drinkIndex) {
        return scores.pairsOf(drinkIndex, thresholdByDrink[drinkIndex]);
    }

    // 상위 경계는 정렬하지 않고 점수를 한 번 훑으며 구한다.
    // TopScores가 누적하는 상태를 스트림 forEach로 바꾸면 부작용이 숨으므로 반복문으로 둔다.
    private static double topThreshold(DrinkAnjuScores scores, int topLimit) {
        TopScores top = TopScores.of(topLimit);
        for (double score : scores.scores()) {
            top.offer(score);
        }

        return top.threshold();
    }

    // 점수를 정렬하지 않고 상위 limit개만 내림차순으로 들고 있다가 limit번째 점수를 경계로 돌려준다.
    // 같은 점수도 하나씩 세므로 정렬 후 limit번째를 고를 때와 경계가 같다.
    // 빈 자리는 음의 무한대로 채워 두므로, 점수가 limit개 미만이면 경계는 음의 무한대가 되어 모두 남는다.
    private record TopScores(double[] scores) {
        private static TopScores of(int limit) {
            if (limit < 1) {
                throw new InvalidTopLimitException();
            }
            double[] scores = new double[limit];
            Arrays.fill(scores, Double.NEGATIVE_INFINITY);

            return new TopScores(scores);
        }

        // 맨 끝 자리부터 더 낮은 점수를 한 칸씩 밀어내고, 끝 자리보다 낮은 점수는 max로 버린다.
        private void offer(double score) {
            int position = scores.length - 1;
            while (position > 0 && scores[position - 1] < score) {
                scores[position] = scores[position - 1];
                position--;
            }
            scores[position] = Math.max(scores[position], score);
        }

        private double threshold() {
            return scores[scores.length - 1];
        }
    }
}
