package com.symphonia.pairing.domain.vo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RelationshipType {
    BOSS(AbvLimit.BOSS_MAX, true),
    FRIEND(AbvLimit.NONE, false),
    COLLEAGUE(AbvLimit.NONE, false),
    LOVER(AbvLimit.NONE, false),
    ;

    private final double maxAbv;
    private final boolean prefersLightAnju;

    public double maxAbv() {
        return maxAbv;
    }

    public boolean prefersLightAnju() {
        return prefersLightAnju;
    }

    private static final class AbvLimit {
        private static final double BOSS_MAX = 12.0;
        private static final double NONE = Double.MAX_VALUE;
    }
}
