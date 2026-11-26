package com.symphonia.gathering.application.recorder;

import com.symphonia.gathering.application.event.ParticipantJoinedEvent.JoinType;

// 모임 흐름의 전환 지표를 건수로 남긴다. 개인정보와 식별자는 받지 않는다.
public interface GatheringMetricRecorder {
    void recordGatheringOpened();

    void recordParticipantJoined(JoinType type);

    void recordSettlementRecorded();

    void recordSharesSent();
}
