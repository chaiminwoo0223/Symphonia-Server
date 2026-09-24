package com.symphonia.pairing.domain.vo;

final class AxisDistance {
    private static final int MAX_AXIS_VALUE = 5;

    private AxisDistance() {}

    static double similarity(int[] a, int[] b) {
        double distanceSquared = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            distanceSquared += diff * diff;
        }

        double maxDistanceSquared = a.length * (double) (MAX_AXIS_VALUE * MAX_AXIS_VALUE);
        return 1 - Math.sqrt(distanceSquared / maxDistanceSquared);
    }
}
