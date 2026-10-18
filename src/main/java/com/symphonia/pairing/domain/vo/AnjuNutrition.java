package com.symphonia.pairing.domain.vo;

import java.util.Optional;

// K-FIND 식품코드 하나의 100g당 영양성분이다. K-FIND가 측정하지 않은 성분은 null이다.
// rawMaterials는 가공식품만 같은 품목제조보고번호의 C002 원재료명을 담고, 음식은 빈 문자열이다.
public record AnjuNutrition(
        String kfindCode,
        String foodName,
        Double sugarGrams,
        Double fatGrams,
        String rawMaterials) {
    public AnjuNutrition {
        rawMaterials = rawMaterials == null ? "" : rawMaterials;
    }

    public Optional<Double> sugar() {
        return Optional.ofNullable(sugarGrams);
    }

    public Optional<Double> fat() {
        return Optional.ofNullable(fatGrams);
    }
}
