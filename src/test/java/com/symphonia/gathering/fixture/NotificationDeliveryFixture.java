package com.symphonia.gathering.fixture;

import com.symphonia.notification.domain.entity.NotificationDelivery;
import com.symphonia.notification.domain.vo.DeliveryStatus;
import com.symphonia.notification.domain.vo.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 아직 보내지 않은 발송 기록이다. 상태, 시도 횟수, 발행 시각, 맡은 시각은 builder로 바꿔 만든다.
@Getter
@RequiredArgsConstructor
public enum NotificationDeliveryFixture {
    SETTLEMENT_NOTICE(NotificationType.SETTLEMENT, LocalDateTime.of(2026, 10, 27, 20, 30)),
    CANCELLATION_NOTICE(NotificationType.CANCELLATION, LocalDateTime.of(2026, 10, 28, 11, 0));

    private final NotificationType type;
    private final LocalDateTime availableAt;

    // 저장 전의 새 발송 기록이다.
    public NotificationDelivery create(
            Long gatheringId, Long participantId, List<Long> settlementIds) {
        return builder(null, gatheringId, participantId, settlementIds).build();
    }

    public NotificationDelivery createWithId(
            Long id, Long gatheringId, Long participantId, List<Long> settlementIds) {
        return builder(id, gatheringId, participantId, settlementIds).build();
    }

    public NotificationDelivery.NotificationDeliveryBuilder builder(
            Long id, Long gatheringId, Long participantId, List<Long> settlementIds) {
        return NotificationDelivery.builder()
                .id(id)
                .gatheringId(gatheringId)
                .participantId(participantId)
                .type(type)
                .settlementIds(settlementIds)
                .status(DeliveryStatus.PENDING)
                .attempts(0)
                .availableAt(availableAt);
    }
}
