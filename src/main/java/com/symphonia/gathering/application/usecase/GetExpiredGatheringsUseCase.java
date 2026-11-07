package com.symphonia.gathering.application.usecase;

import java.util.List;

public interface GetExpiredGatheringsUseCase {
    // 파기 시각이 지난 모임을 잠그지 않고 id 순으로 찾는다. 파기할 때 모임을 잠그고 다시 판단한다.
    List<Long> getExpiredGatheringIds();
}
