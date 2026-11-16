package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.vo.BankAccount;
import com.symphonia.gathering.domain.vo.ShareStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

// 참여자가 결제자 한 명에게 낼 분담금을 정산 건별로 묶는다. "보냈어요"는 이 묶음 단위로 누른다.
// unpaidAmount는 미입금 분담금만 더한 금액이고, 계좌는 송금할 수 있도록 마스킹하지 않는다.
public record PayerSharesResult(
        Long payerParticipantId,
        String payerName,
        String bankName,
        String accountNumber,
        long unpaidAmount,
        boolean hasUnpaidShares,
        List<ShareBreakdownResult> shares) {
    // gatheringSettlements는 차수의 참여자 이름을 구하는 데 쓰는 모임의 정산 건이고, 취소한 정산 건이 섞여 있어도 된다.
    // 따로 읽는 사이에 본인 정산 건이 취소되거나 수정돼도 그 인원이 이름에서 빠지지 않도록 settlements도 합친다.
    public static PayerSharesResult of(
            Participant payer,
            BankAccount bankAccount,
            List<Settlement> settlements,
            Long participantId,
            Gathering gathering,
            List<Settlement> gatheringSettlements) {
        Map<Long, Set<Long>> participantIdsByRoundId =
                SettlementPolicy.getParticipantIdsByRoundId(
                        Stream.concat(gatheringSettlements.stream(), settlements.stream())
                                .toList());
        List<ShareBreakdownResult> shares =
                settlements.stream()
                        .map(
                                settlement ->
                                        ShareBreakdownResult.of(
                                                settlement,
                                                participantId,
                                                gathering.getRound(settlement.getRoundId()),
                                                gathering.getParticipantNames(
                                                        findRoundParticipantIds(
                                                                participantIdsByRoundId,
                                                                settlement.getRoundId()))))
                        .toList();
        List<ShareBreakdownResult> unpaidShares =
                shares.stream().filter(share -> share.status() == ShareStatus.UNPAID).toList();

        return new PayerSharesResult(
                payer.getId(),
                payer.getName(),
                bankAccount.bankName(),
                bankAccount.accountNumber(),
                unpaidShares.stream().mapToLong(ShareBreakdownResult::amount).sum(),
                !unpaidShares.isEmpty(),
                shares);
    }

    // 취소하지 않은 본인 정산 건의 차수는 항상 합집합에 들어 있으므로, 키가 없으면 서버 버그라서 500으로 둔다.
    private static Set<Long> findRoundParticipantIds(
            Map<Long, Set<Long>> participantIdsByRoundId, Long roundId) {
        return Optional.ofNullable(participantIdsByRoundId.get(roundId)).orElseThrow();
    }
}
