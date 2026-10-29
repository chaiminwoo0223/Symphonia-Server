package com.symphonia.gathering.domain.repository;

import java.util.Optional;

public interface ParticipantLinkTokenRepository {
    void save(String value, Long participantId);

    Optional<Long> findParticipantIdByValue(String value);
}
