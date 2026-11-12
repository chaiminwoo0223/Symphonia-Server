package com.symphonia.pairing.domain.vo;

// 피드백 하나를 저장하거나 바꿀 때 조합별 집계의 좋아요와 싫어요 수가 얼마나 바뀌는지다.
public record PairingRatingChange(int likeDelta, int dislikeDelta) {
    public static PairingRatingChange added(PairingRating rating) {
        return switch (rating) {
            case LIKE -> new PairingRatingChange(1, 0);
            case DISLIKE -> new PairingRatingChange(0, 1);
        };
    }

    // 상황이 바뀔 때 이전 상황의 집계에서 이전 평점을 뺀다.
    public static PairingRatingChange removed(PairingRating rating) {
        return new PairingRatingChange(0, 0).minus(added(rating));
    }

    // 평점이 같으면 더한 수와 뺀 수가 같아 변화량이 0이다.
    public static PairingRatingChange between(PairingRating before, PairingRating after) {
        return added(after).minus(added(before));
    }

    public boolean changesCount() {
        return likeDelta != 0 || dislikeDelta != 0;
    }

    private PairingRatingChange minus(PairingRatingChange other) {
        return new PairingRatingChange(
                likeDelta - other.likeDelta, dislikeDelta - other.dislikeDelta);
    }
}
