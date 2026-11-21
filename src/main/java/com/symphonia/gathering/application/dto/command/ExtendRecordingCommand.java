package com.symphonia.gathering.application.dto.command;

public record ExtendRecordingCommand(Long memberId, Long gatheringId) {
    public static ExtendRecordingCommand of(Long memberId, Long gatheringId) {
        return new ExtendRecordingCommand(memberId, gatheringId);
    }
}
