package com.symphonia.gathering.domain.entity;

import com.symphonia.gathering.domain.policy.NotificationPolicy;
import com.symphonia.gathering.domain.vo.DeliveryStatus;
import com.symphonia.gathering.domain.vo.NotificationType;
import com.symphonia.gathering.domain.vo.PayerAmount;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    // 발행했는데 이 시간이 지나도록 발송을 마치지 못했으면 메시지를 잃은 것으로 보고 다시 발행한다.
    private static final Duration REPUBLISH_AFTER = Duration.ofMinutes(5);
    // 컨슈머가 발송을 시작하고 이 시간 안에 끝내지 못하면 죽은 것으로 보고, 다른 컨슈머가 다시 맡을 수 있게 한다.
    private static final Duration CLAIM_LEASE = Duration.ofMinutes(5);
    // 외부 발송 실패 뒤 다시 보내기까지 기다리는 시간이다. 이 횟수를 다 쓰고 한 번 더 실패하면 FAILED로 끝낸다.
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

    // 수신자마다 아직 내지 않은 분담금이 있는 정산 건을 묶어 한 메시지로 보낸다. 0원 분담금은 미입금이 아니라서 빠진다.
    public static List<NotificationDelivery> forSettlement(
            Long gatheringId, List<Settlement> settlements, LocalDateTime now) {
        Map<Long, List<Long>> settlementIdsByParticipantId = new LinkedHashMap<>();
        settlements.forEach(
                settlement ->
                        settlement.getShares().stream()
                                .filter(SettlementShare::isUnpaid)
                                .forEach(
                                        share ->
                                                settlementIdsByParticipantId
                                                        .computeIfAbsent(
                                                                share.getParticipantId(),
                                                                _ -> new ArrayList<>())
                                                        .add(settlement.getId())));

        return settlementIdsByParticipantId.entrySet().stream()
                .map(
                        entry ->
                                pending(
                                        gatheringId,
                                        entry.getKey(),
                                        NotificationType.SETTLEMENT,
                                        entry.getValue(),
                                        now))
                .toList();
    }

    // 취소 알림은 수정 시간 없이 바로 보내되, 보낼 수 없는 시간대면 그다음 10:00에 보낸다.
    public static List<NotificationDelivery> forCancellation(
            Settlement settlement, List<Long> participantIds, LocalDateTime now) {
        return participantIds.stream()
                .map(
                        participantId ->
                                pending(
                                        settlement.getGatheringId(),
                                        participantId,
                                        NotificationType.CANCELLATION,
                                        List.of(settlement.getId()),
                                        NotificationPolicy.sendableAt(now)))
                .toList();
    }

    // 이 시각보다 먼저 발행하고 아직 보내지 못한 발송 기록을 다시 발행한다.
    public static LocalDateTime republishThreshold(LocalDateTime now) {
        return now.minus(REPUBLISH_AFTER);
    }

    // 이 시각보다 먼저 맡은 발송 기록은 맡은 컨슈머가 죽은 것으로 본다. 아직 맡고 있는 발송 기록은 다시 발행하지 않는다.
    public static LocalDateTime claimExpiryThreshold(LocalDateTime now) {
        return now.minus(CLAIM_LEASE);
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

    // 보낼 금액을 결제자별로 합한다. 메시지를 받은 참여자가 결제자마다 얼마를 보낼지 바로 알 수 있게 하기 위해서다.
    // 정산 메시지는 아직 내지 않은 분담금만, 취소 알림은 취소된 정산 건의 분담금만 담는다. 담을 금액이 없으면 보내지 않는다.
    // 결제자 이름은 붙이지 않는다. 담을 금액이 없을 때 참여자 정보를 복호화하는 모임 조회를 건너뛰기 위해서다.
    public Map<Long, Long> amountsByPayerIdFrom(List<Settlement> settlements) {
        return settlements.stream()
                .filter(settlement -> settlementIds.contains(settlement.getId()))
                .filter(this::isAnnounceable)
                .collect(
                        Collectors.groupingBy(
                                Settlement::getPayerParticipantId,
                                LinkedHashMap::new,
                                Collectors.summingLong(
                                        settlement ->
                                                settlement.getShareOf(participantId).getAmount())));
    }

    // 결제자별로 합한 금액에 결제자 이름을 붙인다.
    public static List<PayerAmount> payerAmountsOf(
            Gathering gathering, Map<Long, Long> amountsByPayerId) {
        return amountsByPayerId.entrySet().stream()
                .map(
                        entry ->
                                new PayerAmount(
                                        gathering.getParticipant(entry.getKey()).getName(),
                                        entry.getValue()))
                .toList();
    }

    public List<Long> getSettlementIds() {
        return List.copyOf(settlementIds);
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

    private boolean isAnnounceable(Settlement settlement) {
        return switch (type) {
            case SETTLEMENT ->
                    !settlement.isCancelled() && settlement.hasUnpaidShareOf(participantId);
            case CANCELLATION ->
                    settlement.isCancelled()
                            && settlement.getShareOf(participantId).getAmount() > 0;
        };
    }
}
