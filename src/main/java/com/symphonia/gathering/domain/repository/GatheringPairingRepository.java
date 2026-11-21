package com.symphonia.gathering.domain.repository;

import com.symphonia.gathering.domain.vo.GatheringPairing;
import java.util.Optional;

public interface GatheringPairingRepository {
    // 모임마다 하나라서 이미 있으면 덮어쓴다.
    void save(Long gatheringId, GatheringPairing gatheringPairing);

    // 참여자가 속한 모임의 페어링을 모임과 참여자를 복호화하지 않고 읽는다.
    Optional<GatheringPairing> findByParticipantId(Long participantId);

    // 모임장이 마지막으로 고른 페어링이다. 모임을 파기하기 전에 선택 신호를 남길 때 쓴다.
    Optional<GatheringPairing> findByGatheringId(Long gatheringId);

    // 이 호출이 지웠으면 true다. 다른 파기가 먼저 지웠다면 false다.
    boolean deleteByGatheringId(Long gatheringId);
}
