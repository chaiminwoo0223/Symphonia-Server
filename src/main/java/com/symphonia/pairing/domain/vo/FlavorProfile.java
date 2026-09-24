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
    private static final int MAX_AXIS_VALUE = 5;
    private static final int AXIS_COUNT = 4;
    private static final double MAX_DISTANCE_SQUARED =
            AXIS_COUNT * (double) (MAX_AXIS_VALUE * MAX_AXIS_VALUE);
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
        int sweetnessDiff = this.sweetness - other.sweetness;
        int bitternessDiff = this.bitterness - other.bitterness;
        int carbonationDiff = this.carbonation - other.carbonation;
        int richnessDiff = this.richness - other.richness;

        double distanceSquared =
                (double) sweetnessDiff * sweetnessDiff
                        + (double) bitternessDiff * bitternessDiff
                        + (double) carbonationDiff * carbonationDiff
                        + (double) richnessDiff * richnessDiff;

        return 1 - Math.sqrt(distanceSquared / MAX_DISTANCE_SQUARED);
    }

    public boolean matches(FlavorProfile other) {
        return similarity(other) >= MATCH_THRESHOLD;
    }

    public boolean isLight() {
        return richness <= LIGHT_RICHNESS_THRESHOLD;
    }
}
