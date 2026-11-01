package com.symphonia.gathering.domain.vo;

import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.NonNull;

// 수신자 한 명에게 보내는 메시지의 내용이다. 문구와 링크 주소는 발송 수단마다 달라 NotificationSender 구현체가 정한다.
// 늦게 받아도 어느 술자리의 메시지인지 알 수 있게, 보낸 시점에 기대는 표현 대신 모임 이름과 모임을 만든 날짜를 담는다.
public record NotificationMessage(
        Long deliveryId,
        NotificationType type,
        PhoneNumber recipientPhoneNumber,
        String recipientName,
        String gatheringName,
        LocalDate gatheringDate,
        List<PayerAmount> payerAmounts,
        String linkToken) {
    public NotificationMessage {
        payerAmounts = List.copyOf(payerAmounts);
    }

    public long getTotalAmount() {
        return payerAmounts.stream().mapToLong(PayerAmount::amount).sum();
    }

    // 링크 토큰은 모임을 파기할 때까지 유효한 인증 수단이라, 로그나 예외 메시지에 남지 않게 내보내지 않는다.
    @Override
    public @NonNull String toString() {
        return "NotificationMessage[deliveryId=%d, type=%s, recipient=%s]"
                .formatted(deliveryId, type, recipientPhoneNumber);
    }
}
