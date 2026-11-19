package com.symphonia.gathering.application.dto.command;

import java.util.List;
import java.util.Map;

// 결제자는 바꿀 수 없다. 결제자를 바꾸려면 정산 건을 취소하고 다시 기록한다.
public record UpdateSettlementCommand(
        Long memberId,
        Long gatheringId,
        Long settlementId,
        Long roundId,
        long totalAmount,
        List<Long> participantIds,
        Map<Long, Long> fixedAmounts) {}
