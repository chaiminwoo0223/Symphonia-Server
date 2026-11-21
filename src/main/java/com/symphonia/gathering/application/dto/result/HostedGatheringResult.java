package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.vo.GatheringRetention;
import com.symphonia.gathering.domain.vo.HostedGathering;
import java.time.LocalDateTime;

// 모임장이 연 모임 하나다. deletesAt은 이 모임의 개인정보를 파기할 시각이다.
// 연 시각, 기록 기간, 연장 여부는 파기 시각과 같은 조회에서 읽은 값으로 담아 서로 어긋나지 않게 한다.
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
                retention.openedAt(),
                retention.getRecordingClosesAt(),
                retention.recordingExtended(),
                retention.deletesAt(),
                hostedGathering.participantCount());
    }
}
