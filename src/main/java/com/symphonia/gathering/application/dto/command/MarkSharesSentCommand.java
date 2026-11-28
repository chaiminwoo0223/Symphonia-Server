package com.symphonia.gathering.application.dto.command;

import java.util.Map;

// seenAmounts는 참여자가 화면에서 본 분담금이고, 정산 건 id별 금액이다.
public record MarkSharesSentCommand(
        String linkToken, Long payerParticipantId, Map<Long, Long> seenAmounts) {}
