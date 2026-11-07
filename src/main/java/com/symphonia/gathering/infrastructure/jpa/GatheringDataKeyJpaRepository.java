package com.symphonia.gathering.infrastructure.jpa;

import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatheringDataKeyJpaRepository
        extends JpaRepository<GatheringDataKeyJpaEntity, Long> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "DELETE FROM GatheringDataKeyJpaEntity dataKey WHERE dataKey.gatheringId IN :gatheringIds")
    void deleteAllByGatheringIds(@Param("gatheringIds") Collection<Long> gatheringIds);
}
