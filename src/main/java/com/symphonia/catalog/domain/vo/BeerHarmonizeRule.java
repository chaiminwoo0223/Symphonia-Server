package com.symphonia.catalog.domain.vo;

import java.util.Set;

// BJCP 스타일 하나에 어울리는 음식 계열이다. 맥주는 제품이 아니라 스타일 단위로 음식 계열을 정한다.
public record BeerHarmonizeRule(String bjcpStyleId, Set<Harmonize> harmonizes) {
    public BeerHarmonizeRule {
        harmonizes = Set.copyOf(harmonizes);
    }
}
