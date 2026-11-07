package com.symphonia.notification.domain.repository;

import com.symphonia.notification.domain.entity.NotificationSchedule;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationScheduleRepository {
    NotificationSchedule save(NotificationSchedule notificationSchedule);

    // 모임의 예약을 잠그고 읽고, 아직 없으면 빈 예약을 만든다.
    // 두 결제자가 동시에 기록하거나 발송과 기록이 겹쳐도 같은 행을 차례로 잠그므로, 마지막 기록 시각이 예약에 남는다.
    NotificationSchedule findOrCreateForUpdate(Long gatheringId);

    Optional<NotificationSchedule> findByGatheringId(Long gatheringId);

    // 예약 시각이 지난 모임을 오래 기다린 순서로 찾는다. 모임마다 따로 발송을 시작해, 한 모임이 실패해도 다른 모임은 나간다.
    List<Long> findDueGatheringIds(LocalDateTime now, int limit);

    // 예약 시각이 지난 모임의 예약을 잠그고 찾는다. 다른 서버가 잠근 예약은 건너뛰어, 여러 서버가 같은 예약을 발송하지 않는다.
    Optional<NotificationSchedule> findDueForUpdate(Long gatheringId, LocalDateTime now);

    void deleteAllByGatheringIds(Collection<Long> gatheringIds);
}
