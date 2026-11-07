package com.symphonia.gathering.infrastructure.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatheringJoinTokenJpaRepository
        extends JpaRepository<GatheringJoinTokenJpaEntity, Long> {
    Optional<GatheringJoinTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM GatheringJoinTokenJpaEntity token WHERE token.gatheringId = :gatheringId")
    void deleteAllByGatheringId(@Param("gatheringId") Long gatheringId);
}
