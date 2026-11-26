package com.symphonia.catalog.domain.vo;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

// 재료 이름에 키워드가 들어 있으면 그 재료에 알레르기, 매운맛, 산미, 쓴맛을 붙인다.
// 속성이 없는 재료도 행을 두어, 규칙표에 아직 없는 재료와 구분한다.
// excludedWords는 키워드를 품었지만 다른 재료를 가리키는 단어다. "바게트"와 "멍게"의 "게"가 게 알레르기로 잡히지 않게 한다.
public record IngredientRule(
        String keyword,
        Set<AllergyType> allergyTypes,
        boolean spicy,
        int acidity,
        int bitterness,
        Set<String> excludedWords) {
    public IngredientRule {
        EnumSet<AllergyType> copy = EnumSet.noneOf(AllergyType.class);
        copy.addAll(allergyTypes);
        allergyTypes = Collections.unmodifiableSet(copy);
        excludedWords = Set.copyOf(excludedWords);
    }

    // 원재료명 문장에는 제외 단어와 진짜 재료가 함께 있을 수 있어, 제외 단어만 지운 나머지에서 키워드를 찾는다.
    public boolean matches(String text) {
        String remaining = text;
        for (String excludedWord : excludedWords) {
            remaining = remaining.replace(excludedWord, " ");
        }

        return remaining.contains(keyword);
    }
}
