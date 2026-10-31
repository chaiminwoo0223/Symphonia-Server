package com.symphonia.gathering.domain.entity;

import com.symphonia.gathering.domain.exception.ShareAlreadyConfirmedException;
import com.symphonia.gathering.domain.exception.ShareAlreadySentException;
import com.symphonia.gathering.domain.exception.ShareAlreadyUnpaidException;
import com.symphonia.gathering.domain.exception.ZeroShareStatusUnchangeableException;
import com.symphonia.gathering.domain.vo.ShareStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 결제자를 뺀 참여자 한 명의 분담이다. fixed는 결제자가 금액을 지정했는지를 나타내고, 계산 근거를 보여 줄 때 쓴다.
// 입금 상태를 바꾸는 메서드는 이 분담을 고치지 않고 상태가 바뀐 새 분담을 돌려준다.
// Settlement가 돌려받은 분담으로 자기 목록을 갈아 끼우므로, 바깥에서 불러도 애그리거트는 바뀌지 않는다.
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SettlementShare {
    private Long participantId;
    private long amount;
    private boolean fixed;
    private ShareStatus status;

    public static SettlementShare equal(Long participantId, long amount) {
        return of(participantId, amount, false);
    }

    public static SettlementShare fixed(Long participantId, long amount) {
        return of(participantId, amount, true);
    }

    public boolean isUnpaid() {
        return status == ShareStatus.UNPAID;
    }

    // 참여자가 보냈다고 알렸거나 결제자가 받았다고 확인한 분담이다.
    // 0원 분담은 확인 완료로 시작하고 되돌릴 수 없지만, 돈이 오가지 않으므로 상태와 관계없이 보낸 분담으로 보지 않는다.
    public boolean isSent() {
        return amount > 0 && !isUnpaid();
    }

    public boolean hasStatus(ShareStatus status) {
        return this.status == status;
    }

    public boolean isOwnedBy(Long participantId) {
        return this.participantId.equals(participantId);
    }

    // 참여자가 "보냈어요"를 누르면 미입금 분담만 보냄이 된다.
    public SettlementShare sent() {
        validateNotSent();
        validateNotConfirmed();
        return withStatus(ShareStatus.SENT);
    }

    // 결제자가 보냄을 확인하거나 직접 받은 돈을 처리하므로, 미입금과 보냄 모두 확인 완료가 될 수 있다.
    public SettlementShare confirmed() {
        validateAmountNotZero();
        validateNotConfirmed();
        return withStatus(ShareStatus.CONFIRMED);
    }

    // 결제자가 "아직 안 들어왔어요"로 보냄을 되돌린다.
    // 받은 돈을 돌려주고 정산 건을 취소할 수 있도록 확인 완료도 미입금으로 되돌릴 수 있다.
    public SettlementShare unpaid() {
        validateAmountNotZero();
        validateNotUnpaid();
        return withStatus(ShareStatus.UNPAID);
    }

    // 생일인 사람처럼 낼 돈이 없는 0원 분담은 받을 것도 없으므로 확인 완료로 시작한다.
    private static SettlementShare of(Long participantId, long amount, boolean fixed) {
        return SettlementShare.builder()
                .participantId(participantId)
                .amount(amount)
                .fixed(fixed)
                .status(amount == 0 ? ShareStatus.CONFIRMED : ShareStatus.UNPAID)
                .build();
    }

    private void validateNotSent() {
        if (status == ShareStatus.SENT) {
            throw new ShareAlreadySentException();
        }
    }

    private void validateNotConfirmed() {
        if (status == ShareStatus.CONFIRMED) {
            throw new ShareAlreadyConfirmedException();
        }
    }

    private SettlementShare withStatus(ShareStatus status) {
        return SettlementShare.builder()
                .participantId(participantId)
                .amount(amount)
                .fixed(fixed)
                .status(status)
                .build();
    }

    // 0원 분담은 확인 완료로 시작하고 낼 돈이 없으므로 결제자가 상태를 바꿀 수 없다.
    // 금액이 잘못됐으면 상태가 아니라 정산 건을 수정해 금액을 고친다.
    private void validateAmountNotZero() {
        if (amount == 0) {
            throw new ZeroShareStatusUnchangeableException();
        }
    }

    private void validateNotUnpaid() {
        if (status == ShareStatus.UNPAID) {
            throw new ShareAlreadyUnpaidException();
        }
    }
}
