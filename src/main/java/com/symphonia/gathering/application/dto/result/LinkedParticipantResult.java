package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.vo.GatheringRetention;
import java.time.LocalDateTime;

public record LinkedParticipantResult(Long participantId, LocalDateTime deletesAt) {
    public static LinkedParticipantResult of(Long participantId, GatheringRetention retention) {
        return new LinkedParticipantResult(participantId, retention.deletesAt());
    }
}
