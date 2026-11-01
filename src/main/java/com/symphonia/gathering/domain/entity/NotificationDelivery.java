package com.symphonia.gathering.domain.entity;

import com.symphonia.gathering.domain.policy.NotificationPolicy;
import com.symphonia.gathering.domain.vo.DeliveryStatus;
import com.symphonia.notification.domain.vo.NotificationType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 참여자 한 명에게 보내는 메시지 한 건의 발송 기록이다. 수신자마다 따로 두어, 실패한 수신자에게만 다시 보낸다.
// 보내기 직전에 담을 금액을 다시 계산하므로, 그사이 보냈거나 취소된 분담금은 메시지에 담기지 않는다.
// 외부 발송에 성공한 직후 장애가 나면 같은 수신자가 한 번 더 받을 수 있다. 정산 요청이 아예 오지 않는 것보다 낫기 때문이다.
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationDelivery {
    // 발행했는데 이 시간이 지나도록 발송을 마치지 못했으면 실패 한 번으로 세고, 재시도 간격을 기다린 뒤 다시 발행한다.
    private static final Duration REPUBLISH_AFTER = Duration.ofMinutes(5);
    // 컨슈머가 발송을 시작하고 이 시간 안에 끝내지 못하면 죽은 것으로 보고, 다른 컨슈머가 다시 맡을 수 있게 한다.
    private static final Duration CLAIM_LEASE = Duration.ofMinutes(5);
    // 발송에 실패한 뒤 다시 보내기까지 기다리는 시간이다. 외부 발송 실패와, 발행했는데 보내지 못한 경우를 같은 횟수로 센다.
    // 이 횟수를 다 쓰고 한 번 더 실패하면 FAILED로 끝낸다.
    private static final List<Duration> RETRY_BACKOFFS =
            List.of(
                    Duration.ofMinutes(1),
                    Duration.ofMinutes(5),
                    Duration.ofMinutes(15),
                    Duration.ofMinutes(30));

    private Long id;
    private Long gatheringId;
    private Long participantId;
    private NotificationType type;
    // 메시지에 담을 정산 건이다. 발송 뒤에 기록한 정산 건은 새 메시지로 따로 알리므로, 같은 금액을 두 번 안내하지 않는다.
    private List<Long> settlementIds;
    private DeliveryStatus status;
    private int attempts;
    // 이 시각부터 보낼 수 있다. 보낼 수 없는 시간대이거나 재시도를 기다리는 동안 미뤄진다.
    private LocalDateTime availableAt;
    private LocalDateTime publishedAt;
    private LocalDateTime claimedAt;
    private LocalDateTime sentAt;

    // 수신자 한 명에게 정산 건 여러 개를 묶어 한 메시지로 보낸다. 담을 정산 건은 발송을 시작할 때 gathering이 고른다.
    public static NotificationDelivery forSettlement(
            Long gatheringId, Long participantId, List<Long> settlementIds, LocalDateTime now) {
        return pending(gatheringId, participantId, NotificationType.SETTLEMENT, settlementIds, now);
    }

    // 취소 알림은 수정 시간 없이 바로 보내되, 보낼 수 없는 시간대면 그다음 10:00에 보낸다.
    public static List<NotificationDelivery> forCancellation(
            Long gatheringId, Long settlementId, List<Long> participantIds, LocalDateTime now) {
        return participantIds.stream()
                .map(
                        participantId ->
                                pending(
                                        gatheringId,
                                        participantId,
                                        NotificationType.CANCELLATION,
                                        List.of(settlementId),
                                        NotificationPolicy.sendableAt(now)))
                .toList();
    }

    // 같은 메시지가 두 번 발행됐거나 다른 컨슈머가 보내는 중이면 맡지 않는다.
    public boolean isClaimable(LocalDateTime now) {
        return isPending()
                && !availableAt.isAfter(now)
                && (claimedAt == null || claimedAt.isBefore(claimExpiryThreshold(now)));
    }

    // 보냈거나 컨슈머가 맡아 이미 내용을 만든 발송 기록이다. 그 뒤에 취소된 정산 건은 메시지에서 빠지지 않으므로 취소 알림을 보낸다.
    public boolean hasReachedRecipient() {
        return status == DeliveryStatus.SENT || (isPending() && claimedAt != null);
    }

    // 발행했는데 보내지 못한 채 다시 찾은 발송 기록은 컨슈머가 예상하지 못한 예외로 실패했거나, 메시지를 잃었거나, 큐가 밀려 아직 받지 못한 것이다.
    // 미루거나 외부 발송 실패로 재시도를 기다릴 때는 발행 시각을 비우므로, 이 경우와 섞이지 않는다.
    public boolean isPublished() {
        return publishedAt != null;
    }

    public boolean isPending() {
        return status == DeliveryStatus.PENDING;
    }

    public void claim(LocalDateTime now) {
        claimedAt = now;
    }

    // 보낼 수 없는 시간대에 맡게 되면 그다음 10:00로 미루고, 그때 다시 발행한다.
    public void defer(LocalDateTime now) {
        waitUntil(NotificationPolicy.nextSendingStart(now));
    }

    public void skip() {
        status = DeliveryStatus.SKIPPED;
    }

    public void markSent(LocalDateTime now) {
        status = DeliveryStatus.SENT;
        sentAt = now;
    }

    public void retryLater(LocalDateTime now) {
        RETRY_BACKOFFS.stream()
                .skip(attempts++)
                .findFirst()
                .ifPresentOrElse(
                        backoff -> waitUntil(now.plus(backoff)),
                        () -> status = DeliveryStatus.FAILED);
    }

    public List<Long> getSettlementIds() {
        return List.copyOf(settlementIds);
    }

    // 이 시각보다 먼저 발행하고 아직 보내지 못한 발송 기록은 실패 한 번으로 세고, 재시도 간격을 기다린 뒤 다시 발행한다.
    public static LocalDateTime republishThreshold(LocalDateTime now) {
        return now.minus(REPUBLISH_AFTER);
    }

    // 이 시각보다 먼저 맡은 발송 기록은 맡은 컨슈머가 죽은 것으로 본다. 아직 맡고 있는 발송 기록은 다시 발행하지 않는다.
    public static LocalDateTime claimExpiryThreshold(LocalDateTime now) {
        return now.minus(CLAIM_LEASE);
    }

    private static NotificationDelivery pending(
            Long gatheringId,
            Long participantId,
            NotificationType type,
            List<Long> settlementIds,
            LocalDateTime availableAt) {
        return NotificationDelivery.builder()
                .gatheringId(gatheringId)
                .participantId(participantId)
                .type(type)
                .settlementIds(List.copyOf(settlementIds))
                .status(DeliveryStatus.PENDING)
                .availableAt(availableAt)
                .build();
    }

    // 미뤄진 시각이 되면 폴러가 다시 발행하도록 발행 시각을 비우고, 다른 컨슈머가 맡을 수 있게 맡은 시각도 비운다.
    private void waitUntil(LocalDateTime time) {
        availableAt = time;
        publishedAt = null;
        claimedAt = null;
    }
}
