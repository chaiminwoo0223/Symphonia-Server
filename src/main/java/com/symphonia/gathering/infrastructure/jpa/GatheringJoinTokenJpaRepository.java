package com.symphonia.gathering.infrastructure.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GatheringJoinTokenJpaRepository
        extends JpaRepository<GatheringJoinTokenJpaEntity, Long> {
    Optional<GatheringJoinTokenJpaEntity> findByTokenHash(String tokenHash);
}
