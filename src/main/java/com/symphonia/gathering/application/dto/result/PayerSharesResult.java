package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.vo.BankAccount;
import com.symphonia.gathering.domain.vo.ShareStatus;
import java.util.List;

// 참여자가 결제자 한 명에게 낼 분담을 정산 건별로 묶는다. "보냈어요"는 이 묶음 단위로 누른다.
// unpaidAmount는 미입금 분담만 더한 금액이고, 계좌는 송금할 수 있도록 마스킹하지 않는다.
public record PayerSharesResult(
        Long payerParticipantId,
        String payerName,
        String bankName,
        String accountNumber,
        long unpaidAmount,
        boolean hasUnpaidShares,
        List<ShareBasisResult> shares) {
    public static PayerSharesResult of(
            Participant payer,
            BankAccount bankAccount,
            List<Settlement> settlements,
            Long participantId) {
        List<ShareBasisResult> shares =
                settlements.stream()
                        .map(settlement -> ShareBasisResult.of(settlement, participantId))
                        .toList();
        List<ShareBasisResult> unpaidShares =
                shares.stream().filter(share -> share.status() == ShareStatus.UNPAID).toList();

        return new PayerSharesResult(
                payer.getId(),
                payer.getName(),
                bankAccount.bankName(),
                bankAccount.accountNumber(),
                unpaidShares.stream().mapToLong(ShareBasisResult::amount).sum(),
                !unpaidShares.isEmpty(),
                shares);
    }
}
