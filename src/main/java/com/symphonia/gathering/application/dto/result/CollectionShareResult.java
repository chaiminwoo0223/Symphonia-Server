package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.GatheringRound;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.entity.SettlementShare;
import com.symphonia.gathering.domain.vo.ShareStatus;

// 결제자가 한 참여자에게서 받을 정산 건 하나의 분담금이다. 확인 완료와 미입금 되돌리기에 쓰는 정산 건 id를 함께 담는다.
public record CollectionShareResult(
        Long settlementId, Long roundId, int roundNumber, long amount, ShareStatus status) {
    public static CollectionShareResult of(
            Settlement settlement, SettlementShare share, GatheringRound round) {
        return new CollectionShareResult(
                settlement.getId(),
                round.getId(),
                round.getNumber(),
                share.getAmount(),
                share.getStatus());
    }
}
