package com.symphonia.gathering.application.usecase;

import java.util.List;

public interface GetPurgeCandidatesUseCase {
    // 파기 시각이 지났을 수 있는 모임을 id 순으로 찾는다. 파기 시각은 파기할 때 모임을 잠그고 다시 판단한다.
    List<Long> getPurgeCandidateIds();
}
