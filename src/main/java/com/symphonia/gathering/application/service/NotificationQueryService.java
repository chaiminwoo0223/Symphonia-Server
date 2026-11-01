package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.QueryService;
import com.symphonia.gathering.application.usecase.GetDueNotificationSchedulesUseCase;
import com.symphonia.gathering.domain.repository.NotificationScheduleRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class NotificationQueryService implements GetDueNotificationSchedulesUseCase {
    // 한 번에 발송을 시작할 모임 수다. 남은 모임은 다음 주기에 이어서 보낸다.
    private static final int DUE_SCHEDULE_LIMIT = 100;

    private final NotificationScheduleRepository notificationScheduleRepository;
    private final Clock clock;

    @Override
    public List<Long> getDueGatheringIds() {
        return notificationScheduleRepository.findDueGatheringIds(
                LocalDateTime.now(clock), DUE_SCHEDULE_LIMIT);
    }
}
