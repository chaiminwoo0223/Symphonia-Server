package com.symphonia.pairing.domain.vo;

import java.util.stream.IntStream;

final class AxisDistance {
    private static final int MAX_AXIS_VALUE = 5;

    private AxisDistance() {}

    public static double similarity(int[] first, int[] second) {
        double distanceSquared =
                IntStream.range(0, first.length)
                        .mapToDouble(i -> square(first[i] - second[i]))
                        .sum();
        double maxDistanceSquared = first.length * (double) (MAX_AXIS_VALUE * MAX_AXIS_VALUE);

        return 1 - Math.sqrt(distanceSquared / maxDistanceSquared);
    }

    private static double square(int value) {
        return (double) value * value;
    }
}
