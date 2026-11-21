package com.symphonia.gathering.fixture;

import com.symphonia.gathering.domain.vo.HostedGathering;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum HostedGatheringFixture {
    FRIDAY_DINNER("금요일 회식", 3),
    EMPTY_GATHERING("빈 모임", 0);

    private final String name;
    private final long participantCount;

    public HostedGathering create(Long gatheringId) {
        return new HostedGathering(gatheringId, name, participantCount);
    }
}
