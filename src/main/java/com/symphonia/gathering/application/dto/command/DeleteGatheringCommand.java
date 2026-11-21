package com.symphonia.gathering.application.dto.command;

public record DeleteGatheringCommand(Long memberId, Long gatheringId) {
    public static DeleteGatheringCommand of(Long memberId, Long gatheringId) {
        return new DeleteGatheringCommand(memberId, gatheringId);
    }
}
