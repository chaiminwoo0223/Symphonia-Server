package com.symphonia.notification.application.service;

import com.symphonia.common.annotation.QueryService;
import com.symphonia.notification.application.dto.result.NotificationScheduleResult;
import com.symphonia.notification.application.usecase.GetDueNotificationSchedulesUseCase;
import com.symphonia.notification.application.usecase.GetNotificationScheduleUseCase;
import com.symphonia.notification.domain.repository.NotificationScheduleRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class NotificationQueryService
        implements GetDueNotificationSchedulesUseCase, GetNotificationScheduleUseCase {
    // 한 번에 발송을 시작할 모임 수다. 남은 모임은 다음 주기에 이어서 보낸다.
    private static final int DUE_SCHEDULE_LIMIT = 100;

    private final NotificationScheduleRepository notificationScheduleRepository;
    private final Clock clock;

    @Override
    public List<Long> getDueGatheringIds() {
        return notificationScheduleRepository.findDueGatheringIds(
                LocalDateTime.now(clock), DUE_SCHEDULE_LIMIT);
    }

    // 아직 한 번도 기록하지 않은 모임은 예약 행이 없어 빈 예약으로 돌려준다.
    @Override
    public NotificationScheduleResult getSchedule(Long gatheringId) {
        return notificationScheduleRepository
                .findByGatheringId(gatheringId)
                .map(NotificationScheduleResult::from)
                .orElseGet(NotificationScheduleResult::empty);
    }
}
