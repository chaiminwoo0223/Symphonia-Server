package com.symphonia.catalog.domain.vo;

import java.util.Set;

// BJCP 스타일 하나의 알레르기 유발 성분이다. 주원료가 스타일을 정하므로 제품이 아니라 스타일 단위로 정한다.
// 알레르기가 없는 스타일도 행을 두어, 규칙표에 아직 없는 스타일과 구분한다.
public record BeerAllergyRule(String bjcpStyleId, Set<AllergyType> allergyTypes) {
    public BeerAllergyRule {
        allergyTypes = Set.copyOf(allergyTypes);
    }
}
