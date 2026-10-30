package com.symphonia.gathering.application.dto.command;

import java.util.List;
import java.util.Map;

public record UpdateSettlementCommand(
        String linkToken,
        Long settlementId,
        long totalAmount,
        List<Long> participantIds,
        Map<Long, Long> fixedAmounts) {}
