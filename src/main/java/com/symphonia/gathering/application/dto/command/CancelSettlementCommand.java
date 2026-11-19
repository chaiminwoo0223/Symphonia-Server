package com.symphonia.gathering.application.dto.command;

public record CancelSettlementCommand(Long memberId, Long gatheringId, Long settlementId) {
    public static CancelSettlementCommand of(Long memberId, Long gatheringId, Long settlementId) {
        return new CancelSettlementCommand(memberId, gatheringId, settlementId);
    }
}
