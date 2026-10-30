package com.symphonia.gathering.domain.repository;

import com.symphonia.gathering.domain.entity.Gathering;
import java.util.Optional;

public interface GatheringRepository {
    Gathering save(Gathering gathering);

    Optional<Gathering> findById(Long gatheringId);

    Optional<Gathering> findByParticipantId(Long participantId);
}
