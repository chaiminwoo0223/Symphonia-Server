package com.symphonia.gathering.fixture;

import com.symphonia.gathering.domain.entity.GatheringRound;
import java.util.List;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// id는 저장된 차수의 id를 흉내 낸 값이다. 실제 DB에 저장하는 테스트는 id가 없는 차수(createNew)를 저장하고 채워진 id를 쓴다.
@Getter
@RequiredArgsConstructor
public enum GatheringRoundFixture {
    FIRST(40L, 1),
    SECOND(41L, 2);

    private final Long id;
    private final int number;

    // 저장된 차수다.
    public GatheringRound create() {
        return GatheringRound.builder().id(id).number(number).build();
    }

    // 저장 전의 새 차수다.
    public GatheringRound createNew() {
        return GatheringRound.of(number);
    }

    // 1차부터 count차까지 번호 순서대로 저장된 차수다. id는 번호에 맞춰 늘어난다.
    public static List<GatheringRound> createUpTo(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(
                        number -> GatheringRound.builder().id(100L + number).number(number).build())
                .toList();
    }
}
