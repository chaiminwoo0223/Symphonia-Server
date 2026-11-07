package com.symphonia.gathering.domain.repository;

import com.symphonia.gathering.domain.vo.GatheringRetention;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

// 모임의 파기 시각을 판단하는 데 필요한 값만 읽는다. 참여자 정보를 복호화하지 않으므로 데이터 키를 지운 모임도 읽을 수 있다.
public interface GatheringRetentionRepository {
    Optional<GatheringRetention> findByGatheringId(Long gatheringId);

    Optional<GatheringRetention> findByParticipantId(Long participantId);

    // 파기 스윕이 후보로 읽을 모임을 id 순으로 찾는다.
    List<Long> findGatheringIdsOpenedUntil(LocalDateTime openedAt);

    // 모임을 잠그고 읽고, 다른 서버가 잠근 모임은 건너뛴다. 여러 서버가 같은 모임을 파기하지 않는다.
    // 모임의 정산 건도 잠가, 판단하는 동안 분담금 상태가 바뀌지 않게 한다.
    List<GatheringRetention> findAllByGatheringIdsForUpdate(Collection<Long> gatheringIds);
}
