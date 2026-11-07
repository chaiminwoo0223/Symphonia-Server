package com.symphonia.gathering.fixture;

import com.symphonia.gathering.domain.vo.GatheringRetention;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

// 조회 시점의 파기 여부만 필요한 테스트가 쓰는 파기 시각이다. 파기 시각을 계산하는 규칙은 PurgeGatheringServiceTest가 검증한다.
// EXPIRED는 파기 시각이 된 순간이다.
@RequiredArgsConstructor
public enum GatheringRetentionFixture {
    RETAINED(Duration.ofDays(7), false),
    EXPIRED(Duration.ZERO, false),
    DATA_KEY_DISCARDED(Duration.ofDays(7), true);

    private final Duration remaining;
    private final boolean dataKeyDiscarded;

    public GatheringRetention create(Long gatheringId, LocalDateTime now) {
        return new GatheringRetention(gatheringId, now.plus(remaining), dataKeyDiscarded);
    }
}
