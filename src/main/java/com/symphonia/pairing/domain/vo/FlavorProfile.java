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
    private static final int MAX_RICHNESS = 5;
    private static final int AXIS_COUNT = 5;

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
        int distanceSquared =
                AxisDistance.square(sweetness - other.sweetness)
                        + AxisDistance.square(bitterness - other.bitterness)
                        + AxisDistance.square(carbonation - other.carbonation)
                        + AxisDistance.square(richness - other.richness)
                        + AxisDistance.square(acidity - other.acidity);

        return AxisDistance.similarity(distanceSquared, AXIS_COUNT);
    }

    public boolean matches(FlavorProfile other) {
        return similarity(other) >= MATCH_THRESHOLD;
    }

    public boolean isLight() {
        return richness <= LIGHT_RICHNESS_THRESHOLD;
    }

    // 가벼운 범위를 넘는 무게감을 0~1로 나타낸다. 가벼우면 0, 무게감 4는 0.5, 5는 1이다.
    public double heaviness() {
        if (isLight()) {
            return 0;
        }
        return (double) (richness - LIGHT_RICHNESS_THRESHOLD)
                / (MAX_RICHNESS - LIGHT_RICHNESS_THRESHOLD);
    }

    // 다른 축은 그대로 두고 한 축만 바꾼 새 FlavorProfile을 만든다. 원래 값은 바꾸지 않는다.
    public FlavorProfile withRichness(int richness) {
        return FlavorProfile.of(sweetness, bitterness, carbonation, richness, acidity);
    }

    public FlavorProfile withAcidity(int acidity) {
        return FlavorProfile.of(sweetness, bitterness, carbonation, richness, acidity);
    }
}
