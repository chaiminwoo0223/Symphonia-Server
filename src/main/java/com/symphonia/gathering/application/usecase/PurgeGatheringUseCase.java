package com.symphonia.gathering.application.usecase;

// 모임 하나의 파기를 두 단계로 나눠 각각 커밋한다(#128).
// 데이터 키를 먼저 지워 커밋하면, 물리 삭제가 실패해도 남은 암호문을 복호화할 수 없다.
public interface PurgeGatheringUseCase {
    // 모임을 잠근 뒤에도 파기 시각이 지났으면 데이터 키를 지우고 true를 돌려준다. 데이터 키를 이미 지운 모임도 true다.
    // 다른 서버가 파기 중이거나 그사이 미입금으로 되돌아갔으면 false를 돌려준다.
    boolean discardDataKeyIfExpired(Long gatheringId);

    // 데이터 키를 지운 모임의 나머지 행을 모두 지운다. 파기된 모임의 개인 링크와 참여 링크는 이때 무효가 된다.
    void purge(Long gatheringId);
}
