package com.symphonia.notification.domain.repository;

import com.symphonia.notification.domain.entity.NotificationDelivery;
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

    // 모임의 리마인드 발송 기록을 상태와 관계없이 찾는다. 같은 참여자에게 24시간 안에 리마인드를 두 번 만들지 않는 데 쓴다.
    // 부른 쪽이 모임의 발송 예약을 잠근 뒤 부르므로, 같은 모임의 리마인드를 동시에 만들지 않는다.
    List<NotificationDelivery> findAllRemindersByGatheringId(Long gatheringId);

    // 모임의 리마인드 발송 기록을 상태와 관계없이 잠그고 찾는다. 취소 알림을 받을 참여자를 고르는 데 쓴다.
    // 리마인드는 컨슈머가 맡을 때 담을 정산 건이 정해져, 정산 건으로 찾으면 맡는 중인 리마인드를 놓친다.
    // 그래서 모임의 리마인드를 모두 잠가 맡는 중이면 끝날 때까지 기다리고, 담은 정산 건은 잠근 뒤에 다시 읽어 거른다.
    List<NotificationDelivery> findAllRemindersByGatheringIdForUpdate(Long gatheringId);

    // 발송 기록과 발송 기록이 담은 정산 건을 함께 지운다.
    void deleteAllByGatheringId(Long gatheringId);
}
