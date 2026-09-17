package com.symphonia.pairing.domain.vo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RelationshipType {
    BOSS(AbvLimit.BOSS_MAX, Boost.FORMALITY, Boost.NONE, true),
    FRIEND(AbvLimit.NONE, Boost.NONE, Boost.NONE, false),
    COLLEAGUE(AbvLimit.NONE, Boost.NONE, Boost.NONE, false),
    LOVER(AbvLimit.NONE, Boost.NONE, Boost.ROMANCE, false),
    ;

    private final double maxAbv;
    private final int formalityBoost;
    private final int romanceBoost;
    private final boolean prefersLightAnju;

    public double maxAbv() {
        return maxAbv;
    }

    public int formalityBoost() {
        return formalityBoost;
    }

    public int romanceBoost() {
        return romanceBoost;
    }

    public boolean prefersLightAnju() {
        return prefersLightAnju;
    }

    private static final class AbvLimit {
        private static final double BOSS_MAX = 12.0;
        private static final double NONE = Double.MAX_VALUE;
    }

    private static final class Boost {
        private static final int FORMALITY = 2;
        private static final int ROMANCE = 1;
        private static final int NONE = 0;
    }
}
