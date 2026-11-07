package com.symphonia.gathering.domain.repository;

import com.symphonia.gathering.domain.entity.Gathering;
import java.util.Collection;
import java.util.Optional;

public interface GatheringRepository {
    Gathering save(Gathering gathering);

    Optional<Gathering> findById(Long gatheringId);

    Optional<Gathering> findByParticipantId(Long participantId);

    // 모임별 데이터 키를 지워, 남은 행이나 백업에 암호문이 있어도 복호화할 수 없게 한다(#128).
    void discardDataKeys(Collection<Long> gatheringIds);

    // 모임과 참여자를 지운다. 참여자를 참조하는 행을 먼저 지운 뒤에 부른다.
    void deleteAllByIds(Collection<Long> gatheringIds);
}
