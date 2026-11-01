package com.symphonia.notification.domain.sender;

import com.symphonia.notification.domain.vo.NotificationMessage;

// 알림톡은 사업자등록 전까지 보류하므로, 지금은 로그를 남기는 구현체로 동작한다.
// 보내지 못하면 NotificationSendFailedException을 던진다. 호출한 쪽이 시도 횟수를 남기고 나중에 다시 보낸다.
public interface NotificationSender {
    void send(NotificationMessage message);
}
