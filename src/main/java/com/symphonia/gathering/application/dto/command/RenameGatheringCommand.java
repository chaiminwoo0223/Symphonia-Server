package com.symphonia.gathering.application.dto.command;

public record RenameGatheringCommand(Long memberId, Long gatheringId, String name) {
    public static RenameGatheringCommand of(Long memberId, Long gatheringId, String name) {
        return new RenameGatheringCommand(memberId, gatheringId, name);
    }
}
