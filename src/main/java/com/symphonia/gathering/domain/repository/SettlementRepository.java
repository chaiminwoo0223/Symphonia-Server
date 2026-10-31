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

    // 참여자의 분담금이 있는 정산 건 중 취소하지 않은 것을 기록한 순서대로 찾는다. 참여자가 보낼 분담금을 보여 주는 데 쓴다.
    List<Settlement> findAllByShareParticipantId(Long participantId);

    // 참여자가 그 결제자에게 낼 분담금이 있는, 취소하지 않은 정산 건을 잠그고 찾는다. "보냈어요"를 처리하는 데 쓴다.
    // 여러 참여자가 같은 정산 건에서 동시에 누르면 늦은 요청이 기다렸다가 최신 상태를 읽어, 버전 충돌로 실패하지 않는다.
    List<Settlement> findAllByShareParticipantIdAndPayerForUpdate(
            Long participantId, Long payerParticipantId);
}
