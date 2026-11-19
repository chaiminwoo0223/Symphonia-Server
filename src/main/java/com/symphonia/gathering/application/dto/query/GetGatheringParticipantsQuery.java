package com.symphonia.gathering.application.dto.query;

public record GetGatheringParticipantsQuery(Long memberId, Long gatheringId) {
    public static GetGatheringParticipantsQuery of(Long memberId, Long gatheringId) {
        return new GetGatheringParticipantsQuery(memberId, gatheringId);
    }
}
