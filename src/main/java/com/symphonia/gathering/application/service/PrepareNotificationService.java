package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.dto.result.PreparedNotificationResult;
import com.symphonia.gathering.application.usecase.PrepareNotificationUseCase;
import com.symphonia.gathering.domain.entity.NotificationDelivery;
import com.symphonia.gathering.domain.policy.NotificationPolicy;
import com.symphonia.gathering.domain.repository.NotificationDeliveryRepository;
import com.symphonia.notification.application.dto.command.ComposeNotificationCommand;
import com.symphonia.notification.application.dto.result.NotificationContentResult;
import com.symphonia.notification.application.provider.NotificationContentProvider;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

// 컨슈머가 받은 발송 기록을 맡고, 보내기 직전의 상태로 메시지 내용을 만든다.
// 이 트랜잭션이 커밋돼야 새 링크 토큰의 해시가 저장되므로, 실제 발송은 SendNotificationUseCase가 커밋 뒤에 한다.
// 순서가 반대면 발송에 성공하고도 저장에 실패해, 메시지의 링크가 무효가 될 수 있다.
@CommandService
@RequiredArgsConstructor
public class PrepareNotificationService implements PrepareNotificationUseCase {
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationContentProvider notificationContentProvider;
    private final Clock clock;

    // 발송 기록이 없으면 모임을 파기하며 함께 지운 것이라 보내지 않는다.
    @Override
    public Optional<PreparedNotificationResult> prepare(Long notificationDeliveryId) {
        LocalDateTime now = LocalDateTime.now(clock);

        return notificationDeliveryRepository
                .findByIdForUpdate(notificationDeliveryId)
                .filter(notificationDelivery -> notificationDelivery.isClaimable(now))
                .flatMap(notificationDelivery -> prepareClaimable(notificationDelivery, now));
    }

    // 미루든 건너뛰든 맡든 발송 기록의 상태가 바뀌므로 저장은 여기서 한 번만 한다.
    private Optional<PreparedNotificationResult> prepareClaimable(
            NotificationDelivery notificationDelivery, LocalDateTime now) {
        Optional<PreparedNotificationResult> prepared = compose(notificationDelivery, now);
        notificationDeliveryRepository.save(notificationDelivery);
        return prepared;
    }

    // 보낼 수 없는 시간대면 미루고, 그사이 보냈거나 취소돼 담을 금액이 없으면 건너뛴다.
    private Optional<PreparedNotificationResult> compose(
            NotificationDelivery notificationDelivery, LocalDateTime now) {
        if (!NotificationPolicy.isSendable(now)) {
            notificationDelivery.defer(now);
            return Optional.empty();
        }

        Optional<NotificationContentResult> content =
                notificationContentProvider.compose(
                        ComposeNotificationCommand.of(
                                notificationDelivery.getGatheringId(),
                                notificationDelivery.getParticipantId(),
                                notificationDelivery.getType(),
                                notificationDelivery.getSettlementIds()));
        if (content.isEmpty()) {
            notificationDelivery.skip();
            return Optional.empty();
        }

        notificationDelivery.claim(now);
        return Optional.of(PreparedNotificationResult.of(notificationDelivery, content.get()));
    }
}
