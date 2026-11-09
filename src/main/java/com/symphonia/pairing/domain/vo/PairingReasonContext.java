package com.symphonia.pairing.domain.vo;

import java.util.Objects;

// 추천 사유를 판정할 때 보는 조합과 자리다.
// likedByFeedback은 피드백 점수가 반영됐고 좋아한 조합으로 판정된 조합인지다.
public record PairingReasonContext(
        PairingDrink drink, PairingAnju anju, Occasion occasion, boolean likedByFeedback) {
    public PairingReasonContext {
        Objects.requireNonNull(drink.getId(), "PairingDrink의 id는 null일 수 없습니다.");
        Objects.requireNonNull(anju.getId(), "PairingAnju의 id는 null일 수 없습니다.");
    }

    public static PairingReasonContext of(
            PairingDrink drink, PairingAnju anju, Occasion occasion, boolean likedByFeedback) {
        return new PairingReasonContext(drink, anju, occasion, likedByFeedback);
    }
}
