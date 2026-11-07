package com.symphonia.notification.infrastructure.jpa;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationDeliveryJpaRepository
        extends JpaRepository<NotificationDeliveryJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT delivery FROM NotificationDeliveryJpaEntity delivery
            WHERE delivery.id = :id
            """)
    Optional<NotificationDeliveryJpaEntity> findByIdForUpdate(@Param("id") Long id);

    // 오래 기다린 발송 기록부터 발행한다.
    @Query(
            value =
                    """
                    SELECT * FROM notification_delivery
                    WHERE status = 'PENDING'
                      AND available_at <= :now
                      AND (published_at IS NULL OR published_at < :publishedBefore)
                      AND (claimed_at IS NULL OR claimed_at < :claimedBefore)
                    ORDER BY available_at
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true)
    List<NotificationDeliveryJpaEntity> findPublishableForUpdate(
            @Param("now") LocalDateTime now,
            @Param("publishedBefore") LocalDateTime publishedBefore,
            @Param("claimedBefore") LocalDateTime claimedBefore,
            @Param("limit") int limit);

    @Modifying
    @Query(
            """
            UPDATE NotificationDeliveryJpaEntity delivery
            SET delivery.publishedAt = :publishedAt
            WHERE delivery.id IN :ids
            """)
    void markPublished(
            @Param("ids") Collection<Long> ids, @Param("publishedAt") LocalDateTime publishedAt);

    // 상태로 거르지 않고 모두 잠근다. 조건에 맞지 않던 행은 잠그지 않아, 컨슈머가 맡는 중인 행을 기다리지 않기 때문이다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT delivery FROM NotificationDeliveryJpaEntity delivery
            JOIN delivery.settlementIds settlementId
            WHERE settlementId = :settlementId
              AND delivery.type = com.symphonia.notification.domain.vo.NotificationType.SETTLEMENT
            ORDER BY delivery.id
            """)
    List<NotificationDeliveryJpaEntity> findAllSettlementDeliveriesForUpdate(
            @Param("settlementId") Long settlementId);

    @Query(
            """
            SELECT delivery FROM NotificationDeliveryJpaEntity delivery
            WHERE delivery.gatheringId = :gatheringId
              AND delivery.type = com.symphonia.notification.domain.vo.NotificationType.REMINDER
            ORDER BY delivery.id
            """)
    List<NotificationDeliveryJpaEntity> findAllRemindersByGatheringId(
            @Param("gatheringId") Long gatheringId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT delivery FROM NotificationDeliveryJpaEntity delivery
            WHERE delivery.gatheringId = :gatheringId
              AND delivery.type = com.symphonia.notification.domain.vo.NotificationType.REMINDER
            ORDER BY delivery.id
            """)
    List<NotificationDeliveryJpaEntity> findAllRemindersByGatheringIdForUpdate(
            @Param("gatheringId") Long gatheringId);

    // 발송 기록이 담은 정산 건은 엔티티가 아닌 컬렉션 테이블이라 JPQL로 지울 수 없어 네이티브 쿼리로 지운다.
    // 발송 기록 id를 따로 읽지 않고 서브쿼리로 넘겨, 발송 기록 수와 관계없이 한 번에 지운다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value =
                    """
                    DELETE FROM notification_delivery_settlement
                    WHERE delivery_id IN (
                        SELECT id FROM notification_delivery
                        WHERE gathering_id = :gatheringId)
                    """,
            nativeQuery = true)
    void deleteSettlementIdsByGatheringId(@Param("gatheringId") Long gatheringId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "DELETE FROM NotificationDeliveryJpaEntity delivery"
                    + " WHERE delivery.gatheringId = :gatheringId")
    void deleteAllByGatheringId(@Param("gatheringId") Long gatheringId);
}
