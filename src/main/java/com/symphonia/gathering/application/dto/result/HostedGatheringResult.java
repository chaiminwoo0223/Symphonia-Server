package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.vo.GatheringRetention;
import com.symphonia.gathering.domain.vo.HostedGathering;
import java.time.LocalDateTime;

// 모임장이 연 모임 하나다. deletesAt은 이 모임의 개인정보를 파기할 시각이다.
public record HostedGatheringResult(
        Long gatheringId,
        String name,
        LocalDateTime openedAt,
        LocalDateTime recordingClosesAt,
        boolean recordingExtended,
        LocalDateTime deletesAt,
        long participantCount) {
    public static HostedGatheringResult of(
            HostedGathering hostedGathering, GatheringRetention retention) {
        return new HostedGatheringResult(
                hostedGathering.gatheringId(),
                hostedGathering.name(),
                hostedGathering.openedAt(),
                hostedGathering.getRecordingClosesAt(),
                hostedGathering.recordingExtended(),
                retention.deletesAt(),
                hostedGathering.participantCount());
    }
}
