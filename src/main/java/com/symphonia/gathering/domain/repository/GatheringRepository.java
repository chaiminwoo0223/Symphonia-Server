package com.symphonia.gathering.domain.repository;

import com.symphonia.gathering.domain.entity.Gathering;
import java.util.Optional;

public interface GatheringRepository {
    // 새 차수를 넣는 쿼리가 트랜잭션을 요구하므로, 부르는 쪽이 트랜잭션을 열어 둔 상태여야 한다.
    Gathering save(Gathering gathering);

    Optional<Gathering> findById(Long gatheringId);

    Optional<Gathering> findByParticipantId(Long participantId);

    // 참여자 한 명을 지운다. 참여자를 참조하는 개인 링크 토큰과 계좌를 먼저 지운 뒤에 부른다.
    // 정산 건이 참여자를 참조하는 중이면 지우지 못하고 ParticipantInSettlementException을 던진다.
    void deleteParticipant(Long participantId);

    // 모임별 데이터 키를 지워, 남은 행이나 백업에 암호문이 있어도 복호화할 수 없게 한다(#128).
    void discardDataKey(Long gatheringId);

    // 모임과 참여자를 지운다. 참여자를 참조하는 행을 먼저 지운 뒤에 부른다.
    void delete(Long gatheringId);
}
