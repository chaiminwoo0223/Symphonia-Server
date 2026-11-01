package com.symphonia.notification.infrastructure.jpa;

import com.symphonia.notification.domain.entity.NotificationSchedule;
import com.symphonia.notification.domain.repository.NotificationScheduleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationScheduleRepositoryImpl implements NotificationScheduleRepository {
    private final NotificationScheduleJpaRepository notificationScheduleJpaRepository;

    @Override
    public NotificationSchedule save(NotificationSchedule notificationSchedule) {
        return notificationScheduleJpaRepository
                .save(NotificationScheduleJpaEntity.from(notificationSchedule))
                .toDomain();
    }

    // 방금 만들었거나 이미 있는 행을 잠그므로, 찾지 못하면 서버 버그라서 500으로 둔다.
    @Override
    public NotificationSchedule findOrCreateForUpdate(Long gatheringId) {
        notificationScheduleJpaRepository.insertIfAbsent(gatheringId);

        return notificationScheduleJpaRepository
                .findByGatheringIdForUpdate(gatheringId)
                .orElseThrow()
                .toDomain();
    }

    @Override
    public Optional<NotificationSchedule> findByGatheringId(Long gatheringId) {
        return notificationScheduleJpaRepository
                .findById(gatheringId)
                .map(NotificationScheduleJpaEntity::toDomain);
    }

    @Override
    public List<Long> findDueGatheringIds(LocalDateTime now, int limit) {
        return notificationScheduleJpaRepository.findDueGatheringIds(now, limit);
    }

    @Override
    public Optional<NotificationSchedule> findDueForUpdate(Long gatheringId, LocalDateTime now) {
        return notificationScheduleJpaRepository
                .findDueForUpdate(gatheringId, now)
                .map(NotificationScheduleJpaEntity::toDomain);
    }
}
