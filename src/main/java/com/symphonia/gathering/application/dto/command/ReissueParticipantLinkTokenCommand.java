package com.symphonia.gathering.application.dto.command;

public record ReissueParticipantLinkTokenCommand(
        Long memberId, Long gatheringId, Long participantId) {
    public static ReissueParticipantLinkTokenCommand of(
            Long memberId, Long gatheringId, Long participantId) {
        return new ReissueParticipantLinkTokenCommand(memberId, gatheringId, participantId);
    }
}
