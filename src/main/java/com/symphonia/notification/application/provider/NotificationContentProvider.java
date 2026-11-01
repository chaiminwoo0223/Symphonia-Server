package com.symphonia.notification.application.provider;

import com.symphonia.notification.application.dto.command.ComposeNotificationCommand;
import com.symphonia.notification.application.dto.result.NotificationContentResult;
import java.util.Optional;

// 보내기 직전에 메시지에 담을 내용을 만든다. 금액, 수신자, 모임 정보와 개인 링크 토큰은 gathering이 갖고 있어 gathering이 구현한다.
// 발송 기록을 맡는 트랜잭션 안에서 부른다. 새 링크 토큰의 해시가 이 트랜잭션과 함께 커밋돼야, 발송한 메시지의 링크가 무효가 되지 않기 때문이다.
public interface NotificationContentProvider {
    // 그사이 보냈거나 취소돼 담을 금액이 없으면 비어 있다. 이때는 참여자 정보를 복호화하지 않고 토큰도 발급하지 않는다.
    Optional<NotificationContentResult> compose(ComposeNotificationCommand command);
}
