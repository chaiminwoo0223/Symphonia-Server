package com.symphonia.pairing.domain.vo;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// 피드백 하나를 저장하거나 바꿀 때 전역 집계와 상황별 집계가 바뀌는 수다.
// 상황별 변화는 집계 행을 잠그는 순서가 정해지도록 상황 순서로 담고, 바뀌는 수가 없는 상황은 담지 않는다.
public record PairingFeedbackChange(
        PairingRatingChange ratingChange, List<SituationRatingChange> situationChanges) {
    public static PairingFeedbackChange added(PairingSituation situation, PairingRating rating) {
        PairingRatingChange added = PairingRatingChange.added(rating);

        return new PairingFeedbackChange(
                added, List.of(SituationRatingChange.of(situation, added)));
    }

    // 상황이 같으면 한 상황의 수만 바뀌고, 상황이 바뀌면 이전 상황에서 이전 평점을 빼고 새 상황에 새 평점을 더한다.
    public static PairingFeedbackChange between(
            PairingSituation beforeSituation,
            PairingRating beforeRating,
            PairingSituation afterSituation,
            PairingRating afterRating) {
        PairingRatingChange ratingChange = PairingRatingChange.between(beforeRating, afterRating);
        Stream<SituationRatingChange> situationChanges =
                beforeSituation.equals(afterSituation)
                        ? Stream.of(SituationRatingChange.of(afterSituation, ratingChange))
                        : Stream.of(
                                SituationRatingChange.of(
                                        beforeSituation, PairingRatingChange.removed(beforeRating)),
                                SituationRatingChange.of(
                                        afterSituation, PairingRatingChange.added(afterRating)));

        return new PairingFeedbackChange(
                ratingChange,
                situationChanges
                        .filter(SituationRatingChange::changesCount)
                        .sorted(Comparator.comparing(SituationRatingChange::situation))
                        .toList());
    }

    // 전역 집계에서 바뀌는 수가 없으면 비어 있다.
    public Optional<PairingRatingChange> globalChange() {
        return Optional.of(ratingChange).filter(PairingRatingChange::changesCount);
    }
}
