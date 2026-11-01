package com.symphonia.gathering.infrastructure.sender;

import com.symphonia.gathering.domain.sender.NotificationSender;
import com.symphonia.gathering.domain.vo.NotificationMessage;
import com.symphonia.gathering.infrastructure.sender.config.properties.NotificationLinkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 알림톡은 개인 개발자가 사업자등록 없이 발송할 수 없어 보류하는 동안 쓰는 구현체다. 실제로 보내지 않고 로그만 남긴다.
// 휴대폰은 마스킹하고 이름은 남기지 않는다. 링크 토큰은 모임을 파기할 때까지 유효한 인증 수단이라 링크에서 가린다.
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingNotificationSender implements NotificationSender {
    private static final String MASKED_LINK_TOKEN = "****";

    private final NotificationLinkProperties notificationLinkProperties;

    @Override
    public void send(NotificationMessage message) {
        log.info(
                "[Notification] deliveryId={}, type={}, recipient={}, gatheringDate={}, payers={},"
                        + " totalAmount={}, link={}",
                message.deliveryId(),
                message.type(),
                message.recipientPhoneNumber().masked(),
                message.gatheringDate(),
                message.payerAmounts().size(),
                message.getTotalAmount(),
                notificationLinkProperties.urlOf(MASKED_LINK_TOKEN));
    }
}
