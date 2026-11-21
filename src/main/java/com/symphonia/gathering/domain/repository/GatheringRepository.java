package com.symphonia.gathering.domain.repository;

import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.vo.HostedGathering;
import java.util.List;
import java.util.Optional;

public interface GatheringRepository {
    // 새 차수를 넣는 쿼리가 트랜잭션을 요구하므로, 부르는 쪽이 트랜잭션을 열어 둔 상태여야 한다.
    Gathering save(Gathering gathering);

    Optional<Gathering> findById(Long gatheringId);

    // 모임 행을 잠그고 읽는다. 같은 모임을 잠그는 다른 요청은 이 트랜잭션이 끝날 때까지 기다린다.
    // 부르는 쪽이 트랜잭션을 열어 둔 상태여야 한다.
    Optional<Gathering> findByIdForUpdate(Long gatheringId);

    Optional<Gathering> findByParticipantId(Long participantId);

    // 참여자를 복호화하지 않고 모임장이 목록에서 보는 값만 읽는다. 모임 id가 큰 순서로 돌려준다.
    List<HostedGathering> findAllHostedByIds(List<Long> gatheringIds);

    // 기록 기간을 연장한 것으로 표시한다. 모임을 저장할 때는 연장 여부를 바꾸지 않으므로 이 메서드로만 바꾼다.
    // 부르는 쪽이 트랜잭션을 열어 둔 상태여야 한다.
    void markRecordingExtended(Long gatheringId);

    // 참여자 한 명을 지운다. 참여자를 참조하는 개인 링크 토큰과 피드백 제출 표시를 먼저 지우고, 정산 건에 없는지 확인한 뒤에 부른다.
    void deleteParticipant(Long participantId);

    // 모임별 데이터 키를 지워, 남은 행이나 백업에 암호문이 있어도 복호화할 수 없게 한다(#128).
    void discardDataKey(Long gatheringId);

    // 모임과 참여자를 지운다. 참여자를 참조하는 행을 먼저 지운 뒤에 부른다.
    void delete(Long gatheringId);
}
