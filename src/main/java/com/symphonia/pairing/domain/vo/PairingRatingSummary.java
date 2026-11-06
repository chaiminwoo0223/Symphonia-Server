package com.symphonia.pairing.domain.vo;

// 음료, 안주, 음악 조합 하나에 쌓인 피드백 평점 수다.
public record PairingRatingSummary(
        Long drinkId, Long anjuId, Long musicMoodId, int likeCount, int dislikeCount) {
    public static PairingRatingSummary of(
            Long drinkId, Long anjuId, Long musicMoodId, int likeCount, int dislikeCount) {
        return new PairingRatingSummary(drinkId, anjuId, musicMoodId, likeCount, dislikeCount);
    }
}
