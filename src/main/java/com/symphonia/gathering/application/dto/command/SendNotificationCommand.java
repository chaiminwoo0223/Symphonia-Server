package com.symphonia.gathering.application.dto.command;

import com.symphonia.gathering.application.dto.result.PreparedNotificationResult;
import com.symphonia.gathering.domain.vo.NotificationMessage;
import com.symphonia.gathering.domain.vo.NotificationType;
import com.symphonia.gathering.domain.vo.PayerAmount;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.NonNull;

public record SendNotificationCommand(
        Long notificationDeliveryId,
        NotificationType type,
        String recipientPhoneNumber,
        String recipientName,
        String gatheringName,
        LocalDate gatheringDate,
        List<PayerAmountCommand> payerAmounts,
        String linkToken) {
    public static SendNotificationCommand from(PreparedNotificationResult result) {
        return new SendNotificationCommand(
                result.notificationDeliveryId(),
                result.type(),
                result.recipientPhoneNumber(),
                result.recipientName(),
                result.gatheringName(),
                result.gatheringDate(),
                result.payerAmounts().stream().map(PayerAmountCommand::from).toList(),
                result.linkToken());
    }

    public NotificationMessage toMessage() {
        return new NotificationMessage(
                notificationDeliveryId,
                type,
                new PhoneNumber(recipientPhoneNumber),
                recipientName,
                gatheringName,
                gatheringDate,
                payerAmounts.stream().map(PayerAmountCommand::toPayerAmount).toList(),
                linkToken);
    }

    // 휴대폰, 이름, 링크 토큰이 로그나 예외 메시지에 남지 않게 id와 종류만 내보낸다.
    @Override
    public @NonNull String toString() {
        return "SendNotificationCommand[notificationDeliveryId=%d, type=%s]"
                .formatted(notificationDeliveryId, type);
    }

    public record PayerAmountCommand(String payerName, long amount) {
        public static PayerAmountCommand from(PreparedNotificationResult.PayerAmountResult result) {
            return new PayerAmountCommand(result.payerName(), result.amount());
        }

        public PayerAmount toPayerAmount() {
            return new PayerAmount(payerName, amount);
        }
    }
}
