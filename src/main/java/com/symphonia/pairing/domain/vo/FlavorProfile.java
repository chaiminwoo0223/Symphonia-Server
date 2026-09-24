package com.symphonia.pairing.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlavorProfile {
    private static final double MATCH_THRESHOLD = 0.7;
    private static final int LIGHT_RICHNESS_THRESHOLD = 3;

    private int sweetness;
    private int bitterness;
    private int carbonation;
    private int richness;

    public static FlavorProfile of(int sweetness, int bitterness, int carbonation, int richness) {
        return FlavorProfile.builder()
                .sweetness(sweetness)
                .bitterness(bitterness)
                .carbonation(carbonation)
                .richness(richness)
                .build();
    }

    public double similarity(FlavorProfile other) {
        return AxisDistance.similarity(
                new int[] {sweetness, bitterness, carbonation, richness},
                new int[] {other.sweetness, other.bitterness, other.carbonation, other.richness});
    }

    public boolean matches(FlavorProfile other) {
        return similarity(other) >= MATCH_THRESHOLD;
    }

    public boolean isLight() {
        return richness <= LIGHT_RICHNESS_THRESHOLD;
    }
}
