package com.symphonia.gathering.application.dto.command;

public record RemoveParticipantCommand(Long memberId, Long gatheringId, Long participantId) {
    public static RemoveParticipantCommand of(Long memberId, Long gatheringId, Long participantId) {
        return new RemoveParticipantCommand(memberId, gatheringId, participantId);
    }
}
