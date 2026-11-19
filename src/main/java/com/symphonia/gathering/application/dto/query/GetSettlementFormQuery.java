package com.symphonia.gathering.application.dto.query;

public record GetSettlementFormQuery(Long memberId, Long gatheringId) {
    public static GetSettlementFormQuery of(Long memberId, Long gatheringId) {
        return new GetSettlementFormQuery(memberId, gatheringId);
    }
}
