package com.symphonia.notification.application.dto.result;

import java.util.List;

// 정산 메시지 한 건을 받을 참여자와 그 메시지에 담을 정산 건이다.
public record NotificationTargetResult(Long participantId, List<Long> settlementIds) {
    public NotificationTargetResult {
        settlementIds = List.copyOf(settlementIds);
    }

    public static NotificationTargetResult of(Long participantId, List<Long> settlementIds) {
        return new NotificationTargetResult(participantId, settlementIds);
    }
}
