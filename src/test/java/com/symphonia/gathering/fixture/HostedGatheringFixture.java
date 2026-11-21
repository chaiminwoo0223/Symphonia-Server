package com.symphonia.gathering.fixture;

import com.symphonia.gathering.domain.vo.HostedGathering;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum HostedGatheringFixture {
    FRIDAY_DINNER("금요일 회식", false, 3),
    EXTENDED_FRIDAY_DINNER("금요일 회식", true, 3),
    EMPTY_GATHERING("빈 모임", false, 0);

    private final String name;
    private final boolean recordingExtended;
    private final long participantCount;

    public HostedGathering create(Long gatheringId, LocalDateTime openedAt) {
        return new HostedGathering(
                gatheringId, name, openedAt, recordingExtended, participantCount);
    }
}
