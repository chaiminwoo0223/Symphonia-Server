package com.symphonia.gathering.application.usecase;

import java.util.List;

// 모임 파기는 두 단계로 나눠 각각 커밋한다(#128).
// 데이터 키를 먼저 지워 커밋하면, 물리 삭제가 실패해도 남은 암호문을 복호화할 수 없다.
public interface PurgeGatheringsUseCase {
    // 후보 중 파기 시각이 지난 모임의 데이터 키를 지우고, 그 모임 id를 돌려준다. 데이터 키를 이미 지운 모임도 돌려준다.
    List<Long> discardExpiredDataKeys(List<Long> gatheringIds);

    // 데이터 키를 지운 모임의 나머지 행을 모두 지운다. 파기된 모임의 개인 링크와 참여 링크는 이때 무효가 된다.
    void purge(List<Long> gatheringIds);
}
