package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.GatheringRound;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.vo.ShareStatus;
import java.time.LocalDateTime;
import java.util.List;

// confirmedAmount는 결제자가 받았다고 확인한 금액이고, sentAmount는 참여자가 보냈다고 알렸지만 아직 확인하지 않은 금액이다.
// roundId와 roundNumber는 정산 건이 속한 차수의 id와 번호다.
// receivableAmount는 결제자가 다른 참여자에게서 받을 금액이고, 총액에서 결제자 분담액을 뺀 값이다.
public record SettlementResult(
        Long id,
        Long roundId,
        int roundNumber,
        Long payerParticipantId,
        long totalAmount,
        long payerAmount,
        long receivableAmount,
        long confirmedAmount,
        long sentAmount,
        LocalDateTime recordedAt,
        List<SettlementShareResult> shares) {
    public static SettlementResult of(Settlement settlement, GatheringRound round) {
        return new SettlementResult(
                settlement.getId(),
                round.getId(),
                round.getNumber(),
                settlement.getPayerParticipantId(),
                settlement.getTotalAmount(),
                settlement.getPayerAmount(),
                settlement.getReceivableAmount(),
                settlement.getAmountOf(ShareStatus.CONFIRMED),
                settlement.getAmountOf(ShareStatus.SENT),
                settlement.getRecordedAt(),
                settlement.getShares().stream().map(SettlementShareResult::from).toList());
    }
}
