package com.symphonia.gathering.domain.repository;

import com.symphonia.gathering.domain.entity.Settlement;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository {
    Settlement save(Settlement settlement);

    Optional<Settlement> findById(Long settlementId);

    // 취소한 정산 건은 빼고 가장 나중에 만든 정산 건을 찾는다. 다음 정산 건의 인원 기본값으로 쓴다.
    Optional<Settlement> findLatestByGatheringId(Long gatheringId);

    // 결제자가 기록한 정산 건 중 취소하지 않은 것을 기록한 순서대로 찾는다. 결제자가 고치거나 취소할 정산 건을 고르는 데 쓴다.
    List<Settlement> findAllByPayerParticipantId(Long payerParticipantId);
}
