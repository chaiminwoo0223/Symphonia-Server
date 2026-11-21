package com.symphonia.gathering.domain.repository;

// 참여자가 본인 확인에 쓴 구글 계정을 모임 안에서 하나로 묶는다. 구글 sub 원문은 저장하지 않는다.
public interface ParticipantGoogleSubjectRepository {
    // 같은 구글 계정으로 이미 참여한 참여자가 모임에 있는지 확인한다.
    boolean exists(Long gatheringId, String googleSubject);

    // 참여자를 저장한 뒤에 부른다. 같은 모임에 같은 구글 계정이 이미 있으면 참여자 중복으로 알린다.
    // 부르는 쪽이 트랜잭션을 열어 둔 상태여야 한다.
    void save(Long gatheringId, Long participantId, String googleSubject);
}
