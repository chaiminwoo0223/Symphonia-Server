package com.symphonia.notification.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.notification.application.usecase.PurgeNotificationsUseCase;
import com.symphonia.notification.domain.repository.NotificationDeliveryRepository;
import com.symphonia.notification.domain.repository.NotificationScheduleRepository;
import java.util.Collection;
import lombok.RequiredArgsConstructor;

// 발송 예약을 먼저 지운다. 정산 건을 바꾸는 흐름이 예약을 잠근 뒤 정산 건을 잠그므로, 같은 순서로 잠가 교착 상태가 생기지 않는다.
// 컨슈머가 맡고 있는 발송 기록은 끝날 때까지 기다렸다가 지운다. 지운 뒤에 온 메시지는 발송 기록이 없어 보내지 않는다.
@CommandService
@RequiredArgsConstructor
public class PurgeNotificationsService implements PurgeNotificationsUseCase {
    private final NotificationScheduleRepository notificationScheduleRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;

    @Override
    public void purge(Collection<Long> gatheringIds) {
        notificationScheduleRepository.deleteAllByGatheringIds(gatheringIds);
        notificationDeliveryRepository.deleteAllByGatheringIds(gatheringIds);
    }
}
