package com.symphonia.gathering.domain.policy;

// 분담 방식과 분리한 반올림 정책이다. 단위나 귀속 대상을 바꿔도 SplitMethod는 바뀌지 않는다.
public enum ShareRoundingPolicy {
    ;

    // 결제자를 포함한 shareCount명이 remainingAmount를 똑같이 나눌 때, 결제자를 뺀 한 명의 몫을 돌려준다.
    // 몫은 1원 단위로 내리고, 나누어떨어지지 않아 남는 끝전은 결제자에게 귀속한다.
    // 끝전은 shareCount원보다 적어 따로 정산하지 않는다. 내린 몫의 합은 남은 금액을 넘지 않으므로 결제자 분담액은 0보다 작아지지 않는다.
    public static long equalShare(long remainingAmount, int shareCount) {
        return Math.floorDiv(remainingAmount, shareCount);
    }
}
