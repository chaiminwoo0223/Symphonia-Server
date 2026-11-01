package com.symphonia.notification.application.dto.command;

public record AnnounceCancellationCommand(Long gatheringId, Long settlementId) {
    public static AnnounceCancellationCommand of(Long gatheringId, Long settlementId) {
        return new AnnounceCancellationCommand(gatheringId, settlementId);
    }
}
