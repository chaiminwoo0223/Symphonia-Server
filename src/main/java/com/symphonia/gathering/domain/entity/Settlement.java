package com.symphonia.gathering.domain.entity;

import com.symphonia.gathering.domain.exception.SettlementAlreadyCancelledException;
import com.symphonia.gathering.domain.exception.SettlementAlreadyNotifiedException;
import com.symphonia.gathering.domain.exception.SettlementShareNotFoundException;
import com.symphonia.gathering.domain.exception.SettlementSharesSentException;
import com.symphonia.gathering.domain.vo.ShareStatus;
import com.symphonia.gathering.domain.vo.SplitMethod;
import java.time.LocalDateTime;
import java.util.List;
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
    private Long payerParticipantId;
    private long totalAmount;
    private List<SettlementShare> shares;
    private LocalDateTime recordedAt;
    private LocalDateTime notifiedAt;
    private LocalDateTime cancelledAt;
    // 정산 건을 읽은 뒤 다른 요청이 바꿨는지 저장할 때 확인하는 애그리거트 버전이다. 새 정산 건은 null이다.
    private Long version;

    public static Settlement record(Long gatheringId, SplitMethod splitMethod, LocalDateTime now) {
        return Settlement.builder()
                .gatheringId(gatheringId)
                .payerParticipantId(splitMethod.payerParticipantId())
                .totalAmount(splitMethod.totalAmount())
                .shares(splitMethod.shares())
                .recordedAt(now)
                .build();
    }

    // 이미 받은 메시지의 금액이 알림 없이 바뀌지 않게, 정산 메시지를 보낸 뒤에는 수정을 막는다.
    // 수정하면 분담금을 모두 다시 만들므로, 보낸 기록이 사라지지 않게 보낸 분담금이 있어도 막는다.
    // 발송 시각을 다시 계산할 때 마지막 기록 시각을 쓰므로(#126), 수정하면 기록 시각도 바꾼다.
    public void update(SplitMethod splitMethod, LocalDateTime now) {
        validateNotCancelled();
        validateNotNotified();
        validateNoSentShares();

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

    // 정산 메시지를 보내기 전에도 개인 링크로 분담금을 볼 수 있으므로, 발송 여부와 관계없이 보냄으로 바꿀 수 있다.
    public void markShareSent(Long participantId) {
        validateNotCancelled();
        replaceShare(getShareOf(participantId).sent());
    }

    public void confirmShare(Long participantId) {
        validateNotCancelled();
        replaceShare(getShareOf(participantId).confirmed());
    }

    public void revertShareToUnpaid(Long participantId) {
        validateNotCancelled();
        replaceShare(getShareOf(participantId).unpaid());
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

    public boolean isNotified() {
        return notifiedAt != null;
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

    public long getPayerAmount() {
        return totalAmount - shares.stream().mapToLong(SettlementShare::getAmount).sum();
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

    private void validateNotCancelled() {
        if (isCancelled()) {
            throw new SettlementAlreadyCancelledException();
        }
    }

    private void validateNotNotified() {
        if (isNotified()) {
            throw new SettlementAlreadyNotifiedException();
        }
    }

    private void validateNoSentShares() {
        if (hasSentShares()) {
            throw new SettlementSharesSentException();
        }
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
}
