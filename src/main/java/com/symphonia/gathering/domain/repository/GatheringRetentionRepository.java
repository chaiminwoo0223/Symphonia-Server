package com.symphonia.gathering.domain.repository;

import com.symphonia.gathering.domain.vo.GatheringRetention;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 모임의 파기 시각을 판단하는 데 필요한 값만 읽는다. 참여자 정보를 복호화하지 않으므로 데이터 키를 지운 모임도 읽을 수 있다.
public interface GatheringRetentionRepository {
    Optional<GatheringRetention> findByGatheringId(Long gatheringId);

    Optional<GatheringRetention> findByParticipantId(Long participantId);

    // 이 시각까지 연 모임을 id 순으로 읽는다. 모임마다 따로 조회하지 않도록 정산 건과 데이터 키를 한 번에 읽는다.
    // 파기 스윕이 잠그지 않고 파기할 모임을 고르는 데 쓴다.
    List<GatheringRetention> findAllOpenedUntil(LocalDateTime openedAt);

    // 모임을 잠그고 읽고, 다른 서버가 잠근 모임이면 기다리지 않고 빈 값을 돌려준다. 여러 서버가 같은 모임을 파기하지 않는다.
    // 모임의 정산 건도 잠가, 판단하는 동안 분담금 상태가 바뀌지 않게 한다.
    Optional<GatheringRetention> findByGatheringIdForUpdate(Long gatheringId);
}
