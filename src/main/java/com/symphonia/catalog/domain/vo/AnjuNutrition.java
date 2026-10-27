package com.symphonia.catalog.domain.vo;

import java.util.Optional;

// K-FIND 식품코드 하나의 100g당 영양성분이다. K-FIND가 측정하지 않은 성분은 null이다.
// rawMaterials는 가공식품의 포장 표기에서 가져온 원재료와 알레르기 유발물질을 담는다. 대조 전이거나 음식이면 빈 문자열이다.
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
