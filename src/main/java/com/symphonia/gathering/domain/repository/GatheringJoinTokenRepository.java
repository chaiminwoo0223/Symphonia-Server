package com.symphonia.gathering.domain.repository;

import java.util.Optional;

public interface GatheringJoinTokenRepository {
    // 모임마다 참여 토큰은 하나라서, 재발급하면 기존 토큰을 덮어써 무효로 만든다.
    void save(String value, Long gatheringId);

    Optional<Long> findGatheringIdByValue(String value);
}
