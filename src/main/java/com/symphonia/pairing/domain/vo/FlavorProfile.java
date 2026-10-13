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
    private int acidity;

    public static FlavorProfile of(
            int sweetness, int bitterness, int carbonation, int richness, int acidity) {
        return FlavorProfile.builder()
                .sweetness(sweetness)
                .bitterness(bitterness)
                .carbonation(carbonation)
                .richness(richness)
                .acidity(acidity)
                .build();
    }

    public double similarity(FlavorProfile other) {
        return AxisDistance.similarity(
                new int[] {sweetness, bitterness, carbonation, richness, acidity},
                new int[] {
                    other.sweetness,
                    other.bitterness,
                    other.carbonation,
                    other.richness,
                    other.acidity
                });
    }

    public boolean matches(FlavorProfile other) {
        return similarity(other) >= MATCH_THRESHOLD;
    }

    public boolean isLight() {
        return richness <= LIGHT_RICHNESS_THRESHOLD;
    }

    // 다른 축은 그대로 두고 한 축만 바꾼 새 FlavorProfile을 만든다. 원래 값은 바꾸지 않는다.
    public FlavorProfile withRichness(int richness) {
        return FlavorProfile.of(sweetness, bitterness, carbonation, richness, acidity);
    }

    public FlavorProfile withAcidity(int acidity) {
        return FlavorProfile.of(sweetness, bitterness, carbonation, richness, acidity);
    }
}
