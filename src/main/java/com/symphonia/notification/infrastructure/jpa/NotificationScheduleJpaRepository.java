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

public interface NotificationScheduleJpaRepository
        extends JpaRepository<NotificationScheduleJpaEntity, Long> {
    // 두 요청이 동시에 처음 기록해도 한 행만 생기도록, 이미 있으면 아무것도 하지 않는다.
    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO notification_schedule (gathering_id) VALUES (:gatheringId)
                    ON CONFLICT (gathering_id) DO NOTHING
                    """,
            nativeQuery = true)
    void insertIfAbsent(@Param("gatheringId") Long gatheringId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT schedule FROM NotificationScheduleJpaEntity schedule
            WHERE schedule.gatheringId = :gatheringId
            """)
    Optional<NotificationScheduleJpaEntity> findByGatheringIdForUpdate(
            @Param("gatheringId") Long gatheringId);

    @Query(
            value =
                    """
                    SELECT gathering_id FROM notification_schedule
                    WHERE scheduled_at <= :now
                    ORDER BY scheduled_at
                    LIMIT :limit
                    """,
            nativeQuery = true)
    List<Long> findDueGatheringIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    // 다른 서버가 발송 중인 예약은 기다리지 않고 건너뛴다.
    // 목록을 읽은 뒤 기록이 예약 시각을 늦췄을 수 있으므로 예약 시각을 다시 확인한다.
    @Query(
            value =
                    """
                    SELECT * FROM notification_schedule
                    WHERE gathering_id = :gatheringId
                      AND scheduled_at <= :now
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true)
    Optional<NotificationScheduleJpaEntity> findDueForUpdate(
            @Param("gatheringId") Long gatheringId, @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "DELETE FROM NotificationScheduleJpaEntity schedule"
                    + " WHERE schedule.gatheringId IN :gatheringIds")
    void deleteAllByGatheringIds(@Param("gatheringIds") Collection<Long> gatheringIds);
}
