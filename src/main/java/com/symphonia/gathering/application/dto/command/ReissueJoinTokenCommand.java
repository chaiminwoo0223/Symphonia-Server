package com.symphonia.gathering.application.dto.command;

public record ReissueJoinTokenCommand(Long memberId, Long gatheringId) {
    public static ReissueJoinTokenCommand of(Long memberId, Long gatheringId) {
        return new ReissueJoinTokenCommand(memberId, gatheringId);
    }
}
