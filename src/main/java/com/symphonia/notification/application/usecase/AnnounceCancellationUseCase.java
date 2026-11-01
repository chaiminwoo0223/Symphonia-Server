package com.symphonia.notification.application.usecase;

import com.symphonia.notification.application.dto.command.AnnounceCancellationCommand;

public interface AnnounceCancellationUseCase {
    // 취소된 정산 건이 담긴 정산 메시지를 받은 참여자에게 취소 알림을 남긴다. 취소와 같은 트랜잭션에서 부른다.
    void announceCancellation(AnnounceCancellationCommand command);
}
