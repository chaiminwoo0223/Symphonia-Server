package com.symphonia.gathering.domain.entity;

import com.symphonia.gathering.domain.exception.SettlementAlreadyCancelledException;
import com.symphonia.gathering.domain.exception.SettlementAlreadyNotifiedException;
import com.symphonia.gathering.domain.exception.SettlementPaymentStartedException;
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
// 수정과 취소 규칙은 이 정산 건의 상태만으로 판단하므로 Settlement가 직접 검사한다.
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
    // 발송 시각을 다시 계산할 때 마지막 기록 시각을 쓰므로(#126), 수정하면 기록 시각도 바꾼다.
    public void update(SplitMethod splitMethod, LocalDateTime now) {
        validateNotCancelled();
        validateNotNotified();
        totalAmount = splitMethod.totalAmount();
        shares = splitMethod.shares();
        recordedAt = now;
    }

    // 보낸 돈이 기록 없이 사라지지 않게, 미입금이 아닌 분담이 있으면 취소를 막는다.
    public void cancel(LocalDateTime now) {
        validateNotCancelled();
        validatePaymentNotStarted();

        cancelledAt = now;
    }

    public boolean isCancelled() {
        return cancelledAt != null;
    }

    public boolean isNotified() {
        return notifiedAt != null;
    }

    public boolean hasPaymentStarted() {
        return !shares.stream().allMatch(SettlementShare::isUnpaid);
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

    private void validatePaymentNotStarted() {
        if (hasPaymentStarted()) {
            throw new SettlementPaymentStartedException();
        }
    }
}
