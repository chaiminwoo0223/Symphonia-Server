package com.symphonia.pairing.domain.vo;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 이번 추천에 쓸 수 있는 조합 중 피드백 집계가 있는 조합과 그 피드백 점수다.
// 도수 상한이나 알레르기로 빠진 음료와 안주의 집계는 담지 않는다.
// 집계마다 점수를 어떻게 매기는지는 모르고, 받은 점수 함수를 그대로 적용한다.
public record FeedbackScores(Map<CombinationId, Double> scoreByCombination) {
    public static FeedbackScores of(
            Collection<PairingRatingSummary> ratingSummaries,
            Set<Long> drinkIds,
            Set<Long> anjuIds,
            ToDoubleFunction<PairingRatingSummary> scoring) {
        Map<CombinationId, Double> scoreByCombination =
                ratingSummaries.stream()
                        .filter(PairingRatingSummary::hasEnoughSample)
                        .filter(
                                summary ->
                                        drinkIds.contains(summary.drinkId())
                                                && anjuIds.contains(summary.anjuId()))
                        .collect(
                                Collectors.toMap(
                                        summary ->
                                                new CombinationId(
                                                        summary.drinkId(), summary.anjuId()),
                                        scoring::applyAsDouble));

        return new FeedbackScores(Map.copyOf(scoreByCombination));
    }

    public double scoreOf(CombinationId combination) {
        return scoreByCombination.getOrDefault(combination, 0.0);
    }

    // 감점받은 조합은 피드백이 없을 때의 순위보다 밀려날 수 있고, 제외할 조합은 순위에서 빠진다(#175).
    // 이렇게 밀려나거나 빠지는 조합(displaced) 수만큼 미리 거르는 경계를 넓혀 다음 순위가 그 자리를 채우게 한다.
    // 감점이면서 제외인 조합은 한 번만 센다.
    public int displacedCount(Set<CombinationId> excluded) {
        return (int) displaced(excluded).count();
    }

    // 스타일별 최고와 무알코올 최고는 그 경계에 속한 음료의 밀려나는 조합만큼만 넓히도록 음료별로 센다.
    public Map<Long, Integer> displacedCountByDrink(Set<CombinationId> excluded) {
        return displaced(excluded)
                .collect(
                        Collectors.groupingBy(
                                CombinationId::drinkId, Collectors.summingInt(_ -> 1)));
    }

    private Stream<CombinationId> displaced(Set<CombinationId> excluded) {
        return Stream.concat(penalized(), excluded.stream()).distinct();
    }

    private Stream<CombinationId> penalized() {
        return scoreByCombination.entrySet().stream()
                .filter(entry -> entry.getValue() < 0)
                .map(Map.Entry::getKey);
    }

    public Set<CombinationId> combinations() {
        return scoreByCombination.keySet();
    }
}
