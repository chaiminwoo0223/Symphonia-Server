package com.symphonia.gathering.domain.entity;

import com.symphonia.gathering.domain.exception.SettlementAlreadyCancelledException;
import com.symphonia.gathering.domain.exception.SettlementShareNotFoundException;
import com.symphonia.gathering.domain.exception.SettlementSharesSentException;
import com.symphonia.gathering.domain.vo.ShareStatus;
import com.symphonia.gathering.domain.vo.SplitMethod;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 카드 결제 1건의 정산이다. 정산 건끼리는 상계하지 않는다.
// 수정, 취소, 분담금 상태 변경 규칙은 이 정산 건의 상태만으로 판단하므로 Settlement가 직접 검사한다.
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Settlement {
    private Long id;
    private Long gatheringId;
    private Long roundId;
    private Long payerParticipantId;
    private long totalAmount;
    private List<SettlementShare> shares;
    private LocalDateTime recordedAt;
    private LocalDateTime cancelledAt;
    // 정산 건을 읽은 뒤 다른 요청이 바꿨는지 저장할 때 확인하는 애그리거트 버전이다. 새 정산 건은 null이다.
    private Long version;

    public static Settlement record(
            Long gatheringId, Long roundId, SplitMethod splitMethod, LocalDateTime now) {
        return Settlement.builder()
                .gatheringId(gatheringId)
                .roundId(roundId)
                .payerParticipantId(splitMethod.payerParticipantId())
                .totalAmount(splitMethod.totalAmount())
                .shares(splitMethod.shares())
                .recordedAt(now)
                .build();
    }

    // 수정하면 분담금을 모두 다시 만들므로, 보낸 기록이 사라지지 않게 보낸 분담금이 있으면 막는다.
    // 잘못 고른 차수를 취소 없이 바로잡을 수 있게 차수도 함께 바꾼다.
    public void update(Long roundId, SplitMethod splitMethod, LocalDateTime now) {
        validateNotCancelled();
        validateNoSentShares();

        this.roundId = roundId;
        totalAmount = splitMethod.totalAmount();
        shares = splitMethod.shares();
        recordedAt = now;
    }

    // 보낸 돈이 기록 없이 사라지지 않게, 보낸 분담금이 있으면 취소를 막는다.
    public void cancel(LocalDateTime now) {
        validateNotCancelled();
        validateNoSentShares();

        cancelledAt = now;
    }

    public void markShareSent(Long participantId, LocalDateTime now) {
        validateNotCancelled();
        replaceShare(getShareOf(participantId).sent(now));
    }

    public void confirmShare(Long participantId, LocalDateTime now) {
        validateNotCancelled();
        replaceShare(getShareOf(participantId).confirmed(now));
    }

    public void revertShareToUnpaid(Long participantId, LocalDateTime now) {
        validateNotCancelled();
        replaceShare(getShareOf(participantId).unpaid(now));
    }

    // 취소하지 않은 정산 건에 아직 내지 않은 분담금이 있으면 정산이 끝나지 않은 것이다. 모임의 파기 시각을 정하는 데 쓴다(#128).
    public boolean hasUnpaidShares() {
        return !isCancelled() && shares.stream().anyMatch(SettlementShare::isUnpaid);
    }

    // 정산 건이 마지막으로 바뀐 시각이다. 분담금 상태를 바꾼 시각과 취소한 시각 중 가장 늦은 값이다.
    // 기록이나 수정만 한 정산 건은 바뀐 시각이 없다. 기록과 수정은 기록 기간 안에만 할 수 있어, 파기 시각은 기록 기간이 끝나는 시각으로 정해진다.
    public Optional<LocalDateTime> getLastChangedAt() {
        return Stream.concat(
                        Stream.of(cancelledAt),
                        shares.stream().map(SettlementShare::getStatusChangedAt))
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo);
    }

    public boolean hasUnpaidShareOf(Long participantId) {
        return getShareOf(participantId).isUnpaid();
    }

    public SettlementShare getShareOf(Long participantId) {
        return shares.stream()
                .filter(share -> share.isOwnedBy(participantId))
                .findFirst()
                .orElseThrow(SettlementShareNotFoundException::new);
    }

    public boolean isCancelled() {
        return cancelledAt != null;
    }

    public boolean hasSentShares() {
        return shares.stream().anyMatch(SettlementShare::isSent);
    }

    // 결제자가 받은 금액과 보냈다는 금액을 따로 볼 수 있게 상태별로 합한다.
    public long getAmountOf(ShareStatus status) {
        return shares.stream()
                .filter(share -> share.hasStatus(status))
                .mapToLong(SettlementShare::getAmount)
                .sum();
    }

    // 결제자가 다른 참여자에게서 받을 금액이다. 총액에서 결제자 분담액을 뺀 값과 같다.
    public long getReceivableAmount() {
        return shares.stream().mapToLong(SettlementShare::getAmount).sum();
    }

    public long getPayerAmount() {
        return totalAmount - getReceivableAmount();
    }

    // 결제자를 포함한 인원이다. 다음 정산 건의 인원 기본값으로 쓴다.
    public List<Long> getParticipantIds() {
        return Stream.concat(
                        Stream.of(payerParticipantId),
                        shares.stream().map(SettlementShare::getParticipantId))
                .toList();
    }

    public List<SettlementShare> getShares() {
        return List.copyOf(shares);
    }

    // 분담금은 상태가 바뀐 새 분담금으로 돌아오므로, 같은 참여자의 분담금을 그 분담금으로 갈아 끼운다.
    private void replaceShare(SettlementShare changedShare) {
        shares =
                shares.stream()
                        .map(
                                share ->
                                        share.isOwnedBy(changedShare.getParticipantId())
                                                ? changedShare
                                                : share)
                        .toList();
    }

    private void validateNotCancelled() {
        if (isCancelled()) {
            throw new SettlementAlreadyCancelledException();
        }
    }

    private void validateNoSentShares() {
        if (hasSentShares()) {
            throw new SettlementSharesSentException();
        }
    }
}
