package com.symphonia.gathering.domain.repository;

import com.symphonia.gathering.domain.entity.Settlement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository {
    Settlement save(Settlement settlement);

    Optional<Settlement> findById(Long settlementId);

    // 발송 기록이 담은 정산 건을 한 번에 읽는다. 보내기 직전에 분담금 상태를 다시 확인하는 데 쓴다.
    List<Settlement> findAllByIds(Collection<Long> settlementIds);

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

    // 아직 정산 메시지를 보내지 않은, 취소하지 않은 모임의 정산 건을 id 순으로 잠그고 찾는다. 발송을 시작하는 데 쓴다.
    // 잠그는 동안 "보냈어요"나 취소가 들어오면 기다렸다가 발송 뒤의 상태를 읽는다.
    List<Settlement> findAllUnnotifiedByGatheringIdForUpdate(Long gatheringId);

    // 취소한 정산 건까지 모임의 정산 건을 모두 찾는다. 모임의 파기 시각을 정하는 데 쓴다.
    List<Settlement> findAllByGatheringId(Long gatheringId);

    // 취소한 정산 건까지 여러 모임의 정산 건을 한 번에 찾는다. 파기 스윕이 파기할 모임을 고르는 데 쓴다.
    List<Settlement> findAllByGatheringIds(Collection<Long> gatheringIds);

    // 취소한 정산 건까지 모임의 정산 건을 id 순으로 잠그고 찾는다. 파기하기 직전에 파기 시각을 다시 판단하는 데 쓴다.
    List<Settlement> findAllByGatheringIdForUpdate(Long gatheringId);

    void deleteAllByGatheringId(Long gatheringId);
}
