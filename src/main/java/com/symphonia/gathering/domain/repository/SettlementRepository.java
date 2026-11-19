package com.symphonia.gathering.domain.repository;

import com.symphonia.gathering.domain.entity.Settlement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository {
    Settlement save(Settlement settlement);

    // 같은 결제자가 같은 요청 id로 기록한 정산 건이 이미 있으면 새로 저장하지 않고 그 정산 건을 돌려준다. 정산 건 기록에만 쓴다.
    // 동시에 같은 요청이 오면 늦은 요청은 먼저 저장한 요청이 커밋한 뒤 그 정산 건을 읽는다.
    Settlement saveIfAbsent(Settlement settlement);

    // 같은 결제자가 같은 요청 id로 이미 기록한 정산 건을 찾는다. 같은 요청을 다시 받았을 때 검증보다 먼저 처음 정산 건을 돌려주는 데 쓴다.
    Optional<Settlement> findByPayerParticipantIdAndClientRequestId(
            Long payerParticipantId, UUID clientRequestId);

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

    // 취소한 정산 건까지 모임의 정산 건을 모두 찾는다. 모임의 파기 시각을 정하고, 분담금 화면에서 차수의 참여자 이름을 구하는 데 쓴다.
    List<Settlement> findAllByGatheringId(Long gatheringId);

    // 취소한 정산 건까지 여러 모임의 정산 건을 한 번에 찾는다. 파기 스윕이 파기할 모임을 고르는 데 쓴다.
    List<Settlement> findAllByGatheringIds(Collection<Long> gatheringIds);

    // 취소한 정산 건까지 모임의 정산 건을 id 순으로 잠그고 찾는다. 파기하기 직전에 파기 시각을 다시 판단하는 데 쓴다.
    List<Settlement> findAllByGatheringIdForUpdate(Long gatheringId);

    // 참여자가 취소한 정산 건까지 포함해 어떤 정산 건의 결제자이거나 분담금 인원인지 확인한다. 참여자를 제거할 수 있는지 판단하는 데 쓴다.
    boolean existsByParticipantId(Long participantId);

    void deleteAllByGatheringId(Long gatheringId);
}
