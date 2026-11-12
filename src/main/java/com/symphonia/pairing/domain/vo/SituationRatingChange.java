package com.symphonia.pairing.domain.vo;

// 한 상황의 조합별 집계에서 좋아요와 싫어요 수가 얼마나 바뀌는지다.
public record SituationRatingChange(PairingSituation situation, PairingRatingChange change) {
    public static SituationRatingChange of(PairingSituation situation, PairingRatingChange change) {
        return new SituationRatingChange(situation, change);
    }

    public boolean changesCount() {
        return change.changesCount();
    }
}
