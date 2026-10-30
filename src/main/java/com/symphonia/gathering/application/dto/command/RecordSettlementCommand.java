package com.symphonia.gathering.application.dto.command;

import java.util.List;
import java.util.Map;

// participantIds는 결제자를 포함한 인원이고, fixedAmounts는 결제자가 금액을 지정한 참여자 id와 그 금액이다.
public record RecordSettlementCommand(
        String linkToken,
        long totalAmount,
        List<Long> participantIds,
        Map<Long, Long> fixedAmounts) {}
