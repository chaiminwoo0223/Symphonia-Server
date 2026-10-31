package com.symphonia.gathering.application.dto.command;

public record RevertShareCommand(String linkToken, Long settlementId, Long participantId) {
    public static RevertShareCommand of(String linkToken, Long settlementId, Long participantId) {
        return new RevertShareCommand(linkToken, settlementId, participantId);
    }
}
