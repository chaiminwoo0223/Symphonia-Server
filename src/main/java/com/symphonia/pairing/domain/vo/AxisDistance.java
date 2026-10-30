package com.symphonia.pairing.domain.vo;

import java.util.stream.IntStream;

final class AxisDistance {
    private static final int MAX_AXIS_VALUE = 5;

    private AxisDistance() {}

    public static double similarity(int[] first, int[] second) {
        int distanceSquared =
                IntStream.range(0, first.length).map(i -> square(first[i] - second[i])).sum();

        return similarity(distanceSquared, first.length);
    }

    // 추천은 요청마다 음료×안주 쌍 전체의 맛 유사도를 계산한다. 그래서 축이 고정된 VO는 배열을 만들지 않고
    // 축별 차이의 제곱 합을 바로 넘긴다. 제곱 합은 정수라 배열로 계산할 때와 결과가 같다.
    public static double similarity(int distanceSquared, int axisCount) {
        double maxDistanceSquared = axisCount * (double) (MAX_AXIS_VALUE * MAX_AXIS_VALUE);

        return 1 - Math.sqrt(distanceSquared / maxDistanceSquared);
    }

    public static int square(int value) {
        return value * value;
    }
}
