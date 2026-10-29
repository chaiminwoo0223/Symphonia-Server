package com.symphonia.gathering.infrastructure.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantJpaRepository extends JpaRepository<ParticipantJpaEntity, Long> {
    List<ParticipantJpaEntity> findAllByGatheringIdOrderById(Long gatheringId);
}
