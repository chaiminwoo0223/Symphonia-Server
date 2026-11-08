package com.symphonia.pairing.domain.vo;

// 음료×안주 조합 하나에 쌓인 피드백 평점 수다.
public record PairingRatingSummary(Long drinkId, Long anjuId, int likeCount, int dislikeCount) {
    // 표본이 이보다 적은 조합은 추천 점수에 반영하지 않는다.
    public static final int MINIMUM_SAMPLE = 5;
    // 좋아요와 싫어요를 반씩 이만큼 미리 센 것처럼 계산해, 표본이 적을수록 좋아요 비율을 0.5 쪽으로 당긴다.
    private static final int PRIOR_SAMPLE = 10;

    public static PairingRatingSummary of(
            Long drinkId, Long anjuId, int likeCount, int dislikeCount) {
        return new PairingRatingSummary(drinkId, anjuId, likeCount, dislikeCount);
    }

    public boolean hasEnoughSample() {
        return sample() >= MINIMUM_SAMPLE;
    }

    // 표본이 적을수록 0.5에 가깝고, 표본이 많을수록 실제 좋아요 비율에 가까워진다.
    public double smoothedLikeRatio() {
        return (likeCount + PRIOR_SAMPLE / 2.0) / (sample() + PRIOR_SAMPLE);
    }

    private int sample() {
        return likeCount + dislikeCount;
    }
}
