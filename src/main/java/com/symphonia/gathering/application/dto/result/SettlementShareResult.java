package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.SettlementShare;
import com.symphonia.gathering.domain.vo.ShareStatus;

public record SettlementShareResult(
        Long participantId, long amount, boolean fixed, ShareStatus status) {
    public static SettlementShareResult from(SettlementShare share) {
        return new SettlementShareResult(
                share.getParticipantId(), share.getAmount(), share.isFixed(), share.getStatus());
    }
}
