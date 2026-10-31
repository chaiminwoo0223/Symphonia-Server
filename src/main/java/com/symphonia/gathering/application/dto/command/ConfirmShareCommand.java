package com.symphonia.gathering.application.dto.command;

public record ConfirmShareCommand(String linkToken, Long settlementId, Long participantId) {
    public static ConfirmShareCommand of(String linkToken, Long settlementId, Long participantId) {
        return new ConfirmShareCommand(linkToken, settlementId, participantId);
    }
}
