package com.symphonia.gathering.domain.repository;

import com.symphonia.gathering.domain.entity.NotificationDelivery;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryRepository {
    NotificationDelivery save(NotificationDelivery notificationDelivery);

    List<NotificationDelivery> saveAll(List<NotificationDelivery> notificationDeliveries);

    // 컨슈머가 발송 기록을 맡을 때 잠그고 읽는다. 같은 메시지가 두 번 발행돼도 한 컨슈머만 맡는다.
    Optional<NotificationDelivery> findByIdForUpdate(Long notificationDeliveryId);

    // 보낼 시각이 됐는데 아직 발행하지 않았거나, 발행한 지 오래됐는데 보내지 못한 발송 기록을 잠그고 찾는다.
    // 컨슈머가 맡고 있는 발송 기록과 다른 서버가 발행 중인 발송 기록은 건너뛴다.
    List<NotificationDelivery> findPublishableForUpdate(
            LocalDateTime now,
            LocalDateTime publishedBefore,
            LocalDateTime claimedBefore,
            int limit);

    void markPublished(Collection<Long> notificationDeliveryIds, LocalDateTime publishedAt);

    // 정산 건이 담긴 정산 메시지의 발송 기록을 잠그고 찾는다. 취소 알림을 받을 참여자를 고르는 데 쓴다.
    // 컨슈머가 내용을 만들거나 보내는 중이면 끝날 때까지 기다려, 취소 전의 금액이 담긴 메시지를 놓치지 않는다.
    List<NotificationDelivery> findAllSettlementDeliveriesForUpdate(Long settlementId);
}
