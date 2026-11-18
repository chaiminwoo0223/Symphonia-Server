package com.symphonia.gathering.application.dto.command;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// participantIds는 결제자를 포함한 인원이고, fixedAmounts는 결제자가 금액을 지정한 참여자 id와 그 금액이다.
// clientRequestId는 같은 기록 요청을 다시 보냈는지 가리는 클라이언트의 요청 id다.
public record RecordSettlementCommand(
        String linkToken,
        UUID clientRequestId,
        Long roundId,
        long totalAmount,
        List<Long> participantIds,
        Map<Long, Long> fixedAmounts) {}
