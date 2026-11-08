package com.symphonia.pairing.domain.vo;

final class AxisDistance {
    private static final int MAX_AXIS_VALUE = 5;

    private AxisDistance() {}

    // 추천은 요청마다 음료×안주 쌍 전체의 맛 유사도를 계산한다.
    // 그래서 배열을 만들지 않고 축별 차이의 제곱 합을 바로 받는다.
    public static double similarity(int distanceSquared, int axisCount) {
        double maxDistanceSquared = axisCount * (double) (MAX_AXIS_VALUE * MAX_AXIS_VALUE);

        return 1 - Math.sqrt(distanceSquared / maxDistanceSquared);
    }

    public static int square(int value) {
        return value * value;
    }
}
