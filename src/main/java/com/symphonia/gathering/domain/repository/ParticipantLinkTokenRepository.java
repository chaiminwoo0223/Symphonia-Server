package com.symphonia.gathering.domain.repository;

import java.util.Optional;

public interface ParticipantLinkTokenRepository {
    // 참여자마다 토큰을 여러 개 둘 수 있어, 저장하면 기존 토큰을 덮어쓰지 않고 새 토큰을 더한다.
    void save(String value, Long participantId);

    Optional<Long> findParticipantIdByValue(String value);
}
