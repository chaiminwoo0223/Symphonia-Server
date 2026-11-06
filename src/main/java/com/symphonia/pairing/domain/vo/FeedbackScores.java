package com.symphonia.pairing.domain.vo;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

// 이번 추천에 쓸 수 있는 조합 중 피드백 집계가 있는 조합과 그 피드백 점수다.
// 도수 상한이나 알레르기로 빠진 음료와 안주의 집계는 담지 않는다.
// 집계마다 점수를 어떻게 매기는지는 모르고, 받은 점수 함수를 그대로 적용한다.
public record FeedbackScores(Map<CombinationId, Double> scoreByCombination) {
    public static FeedbackScores of(
            Collection<PairingRatingSummary> ratingSummaries,
            Set<Long> drinkIds,
            Set<Long> anjuIds,
            Set<Long> musicMoodIds,
            ToDoubleFunction<PairingRatingSummary> scoring) {
        Map<CombinationId, Double> scoreByCombination =
                ratingSummaries.stream()
                        .filter(PairingRatingSummary::hasEnoughSample)
                        .filter(
                                summary ->
                                        drinkIds.contains(summary.drinkId())
                                                && anjuIds.contains(summary.anjuId())
                                                && musicMoodIds.contains(summary.musicMoodId()))
                        .collect(
                                Collectors.toMap(
                                        summary ->
                                                new CombinationId(
                                                        summary.drinkId(),
                                                        summary.anjuId(),
                                                        summary.musicMoodId()),
                                        scoring::applyAsDouble));

        return new FeedbackScores(Map.copyOf(scoreByCombination));
    }

    public double scoreOf(CombinationId combination) {
        return scoreByCombination.getOrDefault(combination, 0.0);
    }

    // 감점받은 조합은 피드백이 없을 때의 순위보다 밀려날 수 있다. 그 수만큼 미리 거르는 경계를 넓힌다.
    public int penalizedCount() {
        return (int) scoreByCombination.values().stream().filter(score -> score < 0).count();
    }

    public Set<CombinationId> combinations() {
        return scoreByCombination.keySet();
    }
}
