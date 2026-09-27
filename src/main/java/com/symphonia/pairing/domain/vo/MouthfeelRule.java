package com.symphonia.pairing.domain.vo;

import java.util.Locale;

public record MouthfeelRule(String keyword, FlavorAxis axis, int value) {
    public MouthfeelRule {
        keyword = keyword.toLowerCase(Locale.ROOT);
    }

    // 키워드가 없으면 -1을 반환한다.
    public int positionIn(String mouthfeel) {
        return mouthfeel.indexOf(keyword);
    }
}
