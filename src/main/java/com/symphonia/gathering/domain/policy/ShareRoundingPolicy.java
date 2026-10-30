package com.symphonia.gathering.domain.policy;

// 분담 방식과 분리한 반올림 정책이다. 단위나 귀속 대상을 바꿔도 SplitMethod는 바뀌지 않는다.
public enum ShareRoundingPolicy {
    ;

    private static final long UNIT = 100;

    // 결제자를 포함한 shareCount명이 remainingAmount를 똑같이 나눌 때, 결제자를 뺀 한 명의 몫을 돌려준다.
    // 몫은 100원 단위로 올리고 나머지는 결제자에게 귀속한다.
    // 올린 몫의 합이 남은 금액을 넘으면 결제자가 총액보다 많이 돌려받게 되므로, 100원 단위로 내린 몫으로 바꾼다.
    public static long equalShare(long remainingAmount, int shareCount) {
        long roundedUp = roundUp(remainingAmount, shareCount);
        long payerAmount = remainingAmount - roundedUp * (shareCount - 1);

        return payerAmount >= 0 ? roundedUp : roundDown(remainingAmount, shareCount);
    }

    // amount를 count명이 나눈 몫을 100원 단위로 올린다.
    // 먼저 count로 나누면 소수점 아래를 잃으므로, 단위와 인원을 곱한 값으로 한 번에 나눈다.
    private static long roundUp(long amount, int count) {
        return Math.ceilDiv(amount, UNIT * count) * UNIT;
    }

    private static long roundDown(long amount, int count) {
        return Math.floorDiv(amount, UNIT * count) * UNIT;
    }
}
