package com.symphonia.pairing.domain.vo;

import java.util.Objects;

// 추천 사유를 판정할 때 보는 조합과 자리다.
public record PairingReasonContext(PairingDrink drink, PairingAnju anju, Occasion occasion) {
    public PairingReasonContext {
        Objects.requireNonNull(drink.getId(), "PairingDrink의 id는 null일 수 없습니다.");
        Objects.requireNonNull(anju.getId(), "PairingAnju의 id는 null일 수 없습니다.");
    }

    public static PairingReasonContext of(PairingDrink drink, PairingAnju anju, Occasion occasion) {
        return new PairingReasonContext(drink, anju, occasion);
    }
}
