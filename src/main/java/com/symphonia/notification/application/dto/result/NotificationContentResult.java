package com.symphonia.notification.application.dto.result;

import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.NonNull;

// 수신자 한 명에게 보낼 메시지의 내용이다. 링크 토큰은 해시를 저장한 트랜잭션이 커밋되면 바로 쓸 수 있다.
public record NotificationContentResult(
        String recipientPhoneNumber,
        String recipientName,
        String gatheringName,
        LocalDate gatheringDate,
        List<PayerAmountResult> payerAmounts,
        String linkToken) {
    public NotificationContentResult {
        payerAmounts = List.copyOf(payerAmounts);
    }

    public static NotificationContentResult of(
            String recipientPhoneNumber,
            String recipientName,
            String gatheringName,
            LocalDate gatheringDate,
            List<PayerAmountResult> payerAmounts,
            String linkToken) {
        return new NotificationContentResult(
                recipientPhoneNumber,
                recipientName,
                gatheringName,
                gatheringDate,
                payerAmounts,
                linkToken);
    }

    // 휴대폰, 이름, 링크 토큰이 로그나 예외 메시지에 남지 않게 아무 값도 내보내지 않는다.
    @Override
    public @NonNull String toString() {
        return "NotificationContentResult[]";
    }

    // 결제자 한 명과 그 결제자에게 보낼 금액이다.
    public record PayerAmountResult(String payerName, long amount) {
        public static PayerAmountResult of(String payerName, long amount) {
            return new PayerAmountResult(payerName, amount);
        }
    }
}
