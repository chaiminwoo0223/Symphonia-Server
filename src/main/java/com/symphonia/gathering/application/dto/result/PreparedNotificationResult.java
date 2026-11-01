package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.NotificationDelivery;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.vo.NotificationType;
import com.symphonia.gathering.domain.vo.PayerAmount;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.NonNull;

// 보낼 준비를 마친 메시지의 내용이다. 링크 토큰은 해시를 이미 커밋했으므로 이 메시지의 링크는 바로 쓸 수 있다.
public record PreparedNotificationResult(
        Long notificationDeliveryId,
        NotificationType type,
        String recipientPhoneNumber,
        String recipientName,
        String gatheringName,
        LocalDate gatheringDate,
        List<PayerAmountResult> payerAmounts,
        String linkToken) {
    public static PreparedNotificationResult of(
            NotificationDelivery notificationDelivery,
            Gathering gathering,
            List<PayerAmount> payerAmounts,
            String linkToken) {
        Participant recipient = gathering.getParticipant(notificationDelivery.getParticipantId());

        return new PreparedNotificationResult(
                notificationDelivery.getId(),
                notificationDelivery.getType(),
                recipient.getPhoneNumber().value(),
                recipient.getName(),
                gathering.getName(),
                gathering.getOpenedAt().toLocalDate(),
                payerAmounts.stream().map(PayerAmountResult::from).toList(),
                linkToken);
    }

    // 휴대폰, 이름, 링크 토큰이 로그나 예외 메시지에 남지 않게 id와 종류만 내보낸다.
    @Override
    public @NonNull String toString() {
        return "PreparedNotificationResult[notificationDeliveryId=%d, type=%s]"
                .formatted(notificationDeliveryId, type);
    }

    public record PayerAmountResult(String payerName, long amount) {
        public static PayerAmountResult from(PayerAmount payerAmount) {
            return new PayerAmountResult(payerAmount.payerName(), payerAmount.amount());
        }
    }
}
