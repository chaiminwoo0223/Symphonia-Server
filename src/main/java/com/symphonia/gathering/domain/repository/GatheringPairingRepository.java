package com.symphonia.gathering.domain.repository;

import com.symphonia.gathering.domain.vo.GatheringPairing;
import java.util.Optional;

public interface GatheringPairingRepository {
    // 모임마다 하나라서 이미 있으면 덮어쓴다.
    void save(Long gatheringId, GatheringPairing gatheringPairing);

    // 참여자가 속한 모임의 페어링을 모임과 참여자를 복호화하지 않고 읽는다.
    Optional<GatheringPairing> findByParticipantId(Long participantId);

    void deleteByGatheringId(Long gatheringId);
}
