package com.symphonia.pairing.domain.vo;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

// 재료 이름에 키워드가 들어 있으면 그 재료에 알레르기, 매운맛, 산미, 쓴맛을 붙인다.
// 속성이 없는 재료도 행을 두어, 규칙표에 아직 없는 재료와 구분한다.
public record IngredientRule(
        String keyword, Set<AllergyType> allergyTypes, boolean spicy, int acidity, int bitterness) {
    public IngredientRule {
        EnumSet<AllergyType> copy = EnumSet.noneOf(AllergyType.class);
        copy.addAll(allergyTypes);
        allergyTypes = Collections.unmodifiableSet(copy);
    }

    public boolean matches(String text) {
        return text.contains(keyword);
    }
}
