package com.symphonia.pairing.domain.vo;

import java.util.Locale;

public enum FlavorAxis {
    SWEETNESS,
    BITTERNESS,
    CARBONATION,
    RICHNESS,
    ACIDITY;

    // 규칙표는 FlavorProfile 필드 이름과 같은 소문자로 축을 적는다.
    public static FlavorAxis from(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT));
    }
}
