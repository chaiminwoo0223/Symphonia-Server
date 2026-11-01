package com.symphonia.notification.application.dto.result;

import com.symphonia.notification.application.dto.result.NotificationContentResult.PayerAmountResult;
import com.symphonia.notification.domain.entity.NotificationDelivery;
import com.symphonia.notification.domain.vo.NotificationType;
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
            NotificationDelivery notificationDelivery, NotificationContentResult content) {
        return new PreparedNotificationResult(
                notificationDelivery.getId(),
                notificationDelivery.getType(),
                content.recipientPhoneNumber(),
                content.recipientName(),
                content.gatheringName(),
                content.gatheringDate(),
                content.payerAmounts(),
                content.linkToken());
    }

    // 휴대폰, 이름, 링크 토큰이 로그나 예외 메시지에 남지 않게 id와 종류만 내보낸다.
    @Override
    public @NonNull String toString() {
        return "PreparedNotificationResult[notificationDeliveryId=%d, type=%s]"
                .formatted(notificationDeliveryId, type);
    }
}
