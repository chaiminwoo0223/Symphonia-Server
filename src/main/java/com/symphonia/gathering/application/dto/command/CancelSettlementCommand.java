package com.symphonia.gathering.application.dto.command;

public record CancelSettlementCommand(String linkToken, Long settlementId) {
    public static CancelSettlementCommand of(String linkToken, Long settlementId) {
        return new CancelSettlementCommand(linkToken, settlementId);
    }
}
