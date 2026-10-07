package com.symphonia.pairing.domain.vo;

import java.util.Locale;
import java.util.OptionalInt;

public record MouthfeelRule(String keyword, FlavorAxis axis, int value) {
    public MouthfeelRule {
        keyword = keyword.toLowerCase(Locale.ROOT);
    }

    // 키워드가 처음 나온 위치를 반환한다. 키워드가 없으면 빈 값이다.
    public OptionalInt positionIn(String mouthfeel) {
        int position = mouthfeel.indexOf(keyword);
        return position < 0 ? OptionalInt.empty() : OptionalInt.of(position);
    }
}
