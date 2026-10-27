package com.symphonia.catalog.domain.vo;

import java.util.Set;

// X-Wines의 Harmonize 태그 하나를 추천에 쓰는 음식 계열로 옮긴다.
// 맞는 계열이 없는 태그(Grilled 같은 조리법 등)는 계열을 비워 두고, 규칙표에 없는 태그와 구분한다.
public record WineHarmonizeRule(String tag, Set<Harmonize> harmonizes) {
    public WineHarmonizeRule {
        harmonizes = Set.copyOf(harmonizes);
    }

    public boolean matches(String wineTag) {
        return tag.equals(wineTag);
    }
}
