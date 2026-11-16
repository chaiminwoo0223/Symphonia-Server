package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.GatheringRound;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.entity.SettlementShare;
import com.symphonia.gathering.domain.vo.ShareStatus;
import java.time.LocalDateTime;
import java.util.List;

// 정산 건 하나에서 참여자 본인의 분담금과 그 계산 근거다. 다른 참여자의 분담액은 담지 않는다.
// headcount는 결제자를 포함한 인원이고, fixed는 결제자가 이 참여자의 금액을 지정했는지를 나타낸다.
// roundId와 roundNumber는 정산 건이 속한 차수이고, roundParticipantNames는 그 차수의 참여자 이름이다.
public record ShareBreakdownResult(
        Long settlementId,
        Long roundId,
        int roundNumber,
        List<String> roundParticipantNames,
        long totalAmount,
        int headcount,
        boolean fixed,
        long amount,
        ShareStatus status,
        LocalDateTime recordedAt) {
    public static ShareBreakdownResult of(
            Settlement settlement,
            Long participantId,
            GatheringRound round,
            List<String> roundParticipantNames) {
        SettlementShare share = settlement.getShareOf(participantId);

        return new ShareBreakdownResult(
                settlement.getId(),
                round.getId(),
                round.getNumber(),
                roundParticipantNames,
                settlement.getTotalAmount(),
                settlement.getParticipantIds().size(),
                share.isFixed(),
                share.getAmount(),
                share.getStatus(),
                settlement.getRecordedAt());
    }
}
