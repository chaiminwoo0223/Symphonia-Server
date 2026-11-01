package com.symphonia.notification.application.dto.command;

import java.time.LocalDateTime;

// notifiedAt은 발송 기록을 만드는 쪽의 현재 시각이다. 정산 건의 발송 시각과 발송 기록의 보낼 수 있는 시각을 같은 시각으로 맞춘다.
public record NotifySettlementsCommand(Long gatheringId, LocalDateTime notifiedAt) {
    public static NotifySettlementsCommand of(Long gatheringId, LocalDateTime notifiedAt) {
        return new NotifySettlementsCommand(gatheringId, notifiedAt);
    }
}
