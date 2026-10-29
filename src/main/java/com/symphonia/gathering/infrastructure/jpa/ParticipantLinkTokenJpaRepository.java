package com.symphonia.gathering.infrastructure.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantLinkTokenJpaRepository
        extends JpaRepository<ParticipantLinkTokenJpaEntity, Long> {
    Optional<ParticipantLinkTokenJpaEntity> findByTokenHash(String tokenHash);
}
