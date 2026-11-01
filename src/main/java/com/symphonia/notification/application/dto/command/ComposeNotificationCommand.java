package com.symphonia.notification.application.dto.command;

import com.symphonia.notification.domain.vo.NotificationType;
import java.util.List;

// 발송 기록 하나가 담을 메시지의 종류와 수신자, 정산 건이다.
public record ComposeNotificationCommand(
        Long gatheringId, Long participantId, NotificationType type, List<Long> settlementIds) {
    public ComposeNotificationCommand {
        settlementIds = List.copyOf(settlementIds);
    }

    public static ComposeNotificationCommand of(
            Long gatheringId, Long participantId, NotificationType type, List<Long> settlementIds) {
        return new ComposeNotificationCommand(gatheringId, participantId, type, settlementIds);
    }
}
