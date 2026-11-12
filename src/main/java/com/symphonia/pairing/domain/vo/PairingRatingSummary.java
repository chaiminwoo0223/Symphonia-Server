package com.symphonia.pairing.domain.vo;

// 음료×안주 조합 하나에 쌓인 피드백 평점 수다.
// 전역 수는 모든 상황의 피드백이고, 상황 수는 추천을 요청한 상황(관계 유형과 분위기)에서 받은 피드백이다.
public record PairingRatingSummary(
        Long drinkId,
        Long anjuId,
        int likeCount,
        int dislikeCount,
        int situationLikeCount,
        int situationDislikeCount) {
    // 전역 표본이 이보다 적은 조합은 추천 점수에 반영하지 않는다. 상황별 표본은 문턱으로 쓰지 않는다.
    public static final int MINIMUM_SAMPLE = 5;
    // 좋아요와 싫어요를 반씩 이만큼 미리 센 것처럼 계산해, 표본이 적을수록 좋아요 비율을 사전 비율 쪽으로 당긴다.
    private static final int PRIOR_SAMPLE = 10;
    // 보정한 좋아요 비율이 이 값 이상이어야 "다른 사람들이 좋아한 조합"으로 본다.
    private static final double LIKED_RATIO = 0.6;

    // 상황별 피드백이 아직 없는 조합이다.
    public static PairingRatingSummary of(
            Long drinkId, Long anjuId, int likeCount, int dislikeCount) {
        return of(drinkId, anjuId, likeCount, dislikeCount, 0, 0);
    }

    public static PairingRatingSummary of(
            Long drinkId,
            Long anjuId,
            int likeCount,
            int dislikeCount,
            int situationLikeCount,
            int situationDislikeCount) {
        return new PairingRatingSummary(
                drinkId,
                anjuId,
                likeCount,
                dislikeCount,
                situationLikeCount,
                situationDislikeCount);
    }

    public boolean hasEnoughSample() {
        return globalSample() >= MINIMUM_SAMPLE;
    }

    // 상황별 표본이 적을수록 전역 비율에 가깝고, 많을수록 그 상황의 실제 좋아요 비율에 가깝다.
    // 상황별 표본이 없으면 전역 비율과 같아 상황별 집계가 쌓이기 전에는 추천이 바뀌지 않는다.
    public double smoothedLikeRatio() {
        return (situationLikeCount + PRIOR_SAMPLE * globalSmoothedLikeRatio())
                / (situationSample() + PRIOR_SAMPLE);
    }

    public boolean isLiked() {
        return smoothedLikeRatio() >= LIKED_RATIO;
    }

    private int globalSample() {
        return likeCount + dislikeCount;
    }

    // 표본이 적을수록 0.5에 가깝고, 표본이 많을수록 실제 좋아요 비율에 가까워진다.
    private double globalSmoothedLikeRatio() {
        return (likeCount + PRIOR_SAMPLE / 2.0) / (globalSample() + PRIOR_SAMPLE);
    }

    private int situationSample() {
        return situationLikeCount + situationDislikeCount;
    }
}
