package com.symphonia.gathering.domain.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 모임 안의 술자리 단위다. 번호는 1부터 빈틈없이 이어지고, 정산 건은 차수 하나에 속한다.
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GatheringRound {
    private static final int FIRST_NUMBER = 1;

    private Long id;
    private int number;

    public static GatheringRound first() {
        return of(FIRST_NUMBER);
    }

    public static GatheringRound of(int number) {
        return GatheringRound.builder().number(number).build();
    }
}
