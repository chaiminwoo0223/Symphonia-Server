package com.symphonia.gathering.fixture;

import com.symphonia.gathering.domain.vo.GatheringRetention;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

// 조회 시점의 파기 여부만 필요한 테스트가 쓰는 파기 시각이다. 파기 시각을 계산하는 규칙은 PurgeGatheringServiceTest가 검증한다.
// EXPIRED는 파기 시각이 된 순간이고, ABOUT_TO_EXPIRE는 파기 시각 1초 전이다.
@RequiredArgsConstructor
public enum GatheringRetentionFixture {
    RETAINED(Duration.ofDays(7), false, false),
    EXTENDED(Duration.ofDays(7), true, false),
    ABOUT_TO_EXPIRE(Duration.ofSeconds(1), false, false),
    EXPIRED(Duration.ZERO, false, false),
    DATA_KEY_DISCARDED(Duration.ofDays(7), false, true);

    private final Duration remaining;
    private final boolean recordingExtended;
    private final boolean dataKeyDiscarded;

    // 연 시각은 만든 시각으로 둔다.
    public GatheringRetention create(Long gatheringId, LocalDateTime now) {
        return new GatheringRetention(
                gatheringId, now, recordingExtended, now.plus(remaining), dataKeyDiscarded);
    }
}
