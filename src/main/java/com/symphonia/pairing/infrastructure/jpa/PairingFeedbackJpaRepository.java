package com.symphonia.pairing.infrastructure.jpa;

import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PairingFeedbackJpaRepository
        extends JpaRepository<PairingFeedbackJpaEntity, Long> {
    @Query("select distinct f.anjuId from PairingFeedbackJpaEntity f where f.anjuId in :anjuIds")
    Set<Long> findReferencedAnjuIds(@Param("anjuIds") Collection<Long> anjuIds);
}
