package com.symphonia.gathering.domain.repository;

import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.vo.HostedGathering;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GatheringRepository {
    // 새 차수를 넣는 쿼리가 트랜잭션을 요구하므로, 부르는 쪽이 트랜잭션을 열어 둔 상태여야 한다.
    Gathering save(Gathering gathering);

    // 같은 모임장과 요청 id의 모임이 이미 있으면 새로 저장하지 않고 빈 값을 돌려준다. 모임 열기에만 쓴다.
    // 동시에 같은 요청이 오면 늦은 요청은 먼저 저장한 요청이 커밋한 뒤 빈 값을 받는다.
    // 부르는 쪽이 트랜잭션을 열어 둔 상태여야 한다.
    Optional<Gathering> saveIfAbsent(Gathering gathering);

    // 같은 모임장이 같은 요청 id로 이미 연 모임을 찾는다. 같은 요청을 다시 받았을 때 새로 열지 않고 처음 모임을 돌려주는 데 쓴다.
    Optional<Gathering> findByHostMemberIdAndClientRequestId(
            Long hostMemberId, UUID clientRequestId);

    Optional<Gathering> findById(Long gatheringId);

    // 모임 행을 잠그고 읽는다. 같은 모임을 잠그는 다른 요청은 이 트랜잭션이 끝날 때까지 기다린다.
    // 부르는 쪽이 트랜잭션을 열어 둔 상태여야 한다.
    Optional<Gathering> findByIdForUpdate(Long gatheringId);

    Optional<Gathering> findByParticipantId(Long participantId);

    // 모임 행을 잠그고 모임장의 회원 id만 읽는다. 참여자를 복호화하지 않으므로 데이터 키를 지운 모임도 읽을 수 있다.
    // 같은 모임을 잠그는 다른 요청은 이 트랜잭션이 끝날 때까지 기다린다. 부르는 쪽이 트랜잭션을 열어 둔 상태여야 한다.
    Optional<Long> findHostMemberIdByIdForUpdate(Long gatheringId);

    // 참여자를 복호화하지 않고 모임장이 목록에서 보는 값만 읽는다. 모임 id가 큰 순서로 돌려준다.
    List<HostedGathering> findAllHostedByIds(List<Long> gatheringIds);

    // 모임 이름을 바꾼다. 모임을 저장할 때는 이름을 바꾸지 않으므로 이 메서드로만 바꾼다.
    // 부르는 쪽이 트랜잭션을 열어 둔 상태여야 한다.
    void updateName(Long gatheringId, String name);

    // 기록 기간을 연장한 것으로 표시한다. 모임을 저장할 때는 연장 여부를 바꾸지 않으므로 이 메서드로만 바꾼다.
    // 부르는 쪽이 트랜잭션을 열어 둔 상태여야 한다.
    void markRecordingExtended(Long gatheringId);

    // 참여자의 이름과 휴대폰을 바꾼다. 모임을 저장할 때는 기존 참여자를 다시 쓰지 않으므로 이 메서드로만 바꾼다.
    // 같은 휴대폰의 다른 참여자가 있으면 참여자 중복으로 알린다. 부르는 쪽이 트랜잭션을 열어 둔 상태여야 한다.
    void updateParticipant(Long gatheringId, Participant participant);

    // 참여자 한 명을 지운다. 참여자를 참조하는 개인 링크 토큰과 피드백 제출 표시를 먼저 지우고, 정산 건에 없는지 확인한 뒤에 부른다.
    void deleteParticipant(Long participantId);

    // 참여자의 이름, 휴대폰, 구글 계정 값을 비우고 철회 시각을 남긴다. 취소한 정산 건이 참여자를 참조해 지울 수 없을 때 쓴다.
    // 익명화한 참여자는 모임을 읽을 때 불러오지 않는다. 참여자를 참조하는 개인 링크 토큰과 피드백 제출 표시를 먼저 지우고 부른다.
    // 부르는 쪽이 트랜잭션을 열어 둔 상태여야 한다.
    void anonymizeParticipant(Long participantId, LocalDateTime withdrawnAt);

    // 모임별 데이터 키를 지워, 남은 행이나 백업에 암호문이 있어도 복호화할 수 없게 한다(#128).
    void discardDataKey(Long gatheringId);

    // 모임과 참여자를 지운다. 참여자를 참조하는 행을 먼저 지운 뒤에 부른다.
    void delete(Long gatheringId);
}
