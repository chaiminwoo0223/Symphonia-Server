package com.symphonia.pairing.domain.vo;

import java.util.List;

// 안내를 판정할 때 보는 자리와 추천 결과다.
// 후보가 없었는지는 카탈로그가 도수 상한으로 거른 음료와 알레르기를 거치기 전의 안주로 가린다.
public record PairingNoticeContext(
        Occasion occasion,
        boolean hasDrinkWithinAbvLimit,
        boolean hasAnjuWithoutAllergen,
        boolean recommended,
        boolean hasNonAlcoholicPairing) {
    public static PairingNoticeContext of(
            List<PairingDrink> drinks,
            List<PairingAnju> anjus,
            Occasion occasion,
            List<Pairing> pairings) {
        return new PairingNoticeContext(
                occasion,
                drinks.stream().anyMatch(occasion::allowsAbvOf),
                anjus.stream().anyMatch(anju -> !occasion.hasAllergyConflict(anju)),
                !pairings.isEmpty(),
                pairings.stream().anyMatch(pairing -> pairing.getDrink().isNonAlcoholic()));
    }
}
