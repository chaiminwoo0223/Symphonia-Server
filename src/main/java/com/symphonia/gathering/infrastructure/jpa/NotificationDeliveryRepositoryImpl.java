package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.entity.NotificationDelivery;
import com.symphonia.gathering.domain.repository.NotificationDeliveryRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationDeliveryRepositoryImpl implements NotificationDeliveryRepository {
    private final NotificationDeliveryJpaRepository notificationDeliveryJpaRepository;

    @Override
    public NotificationDelivery save(NotificationDelivery notificationDelivery) {
        return notificationDeliveryJpaRepository
                .save(NotificationDeliveryJpaEntity.from(notificationDelivery))
                .toDomain();
    }

    @Override
    public List<NotificationDelivery> saveAll(List<NotificationDelivery> notificationDeliveries) {
        return notificationDeliveryJpaRepository
                .saveAll(
                        notificationDeliveries.stream()
                                .map(NotificationDeliveryJpaEntity::from)
                                .toList())
                .stream()
                .map(NotificationDeliveryJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<NotificationDelivery> findByIdForUpdate(Long notificationDeliveryId) {
        return notificationDeliveryJpaRepository
                .findByIdForUpdate(notificationDeliveryId)
                .map(NotificationDeliveryJpaEntity::toDomain);
    }

    @Override
    public List<Long> findPublishableIdsForUpdate(
            LocalDateTime now,
            LocalDateTime publishedBefore,
            LocalDateTime claimedBefore,
            int limit) {
        return notificationDeliveryJpaRepository.findPublishableIdsForUpdate(
                now, publishedBefore, claimedBefore, limit);
    }

    @Override
    public void markPublished(Collection<Long> notificationDeliveryIds, LocalDateTime publishedAt) {
        if (notificationDeliveryIds.isEmpty()) {
            return;
        }
        notificationDeliveryJpaRepository.markPublished(notificationDeliveryIds, publishedAt);
    }

    @Override
    public List<NotificationDelivery> findAllSettlementDeliveriesForUpdate(Long settlementId) {
        return notificationDeliveryJpaRepository
                .findAllSettlementDeliveriesForUpdate(settlementId)
                .stream()
                .map(NotificationDeliveryJpaEntity::toDomain)
                .toList();
    }
}
