package com.symphonia.gathering.infrastructure.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementShareJpaRepository
        extends JpaRepository<SettlementShareJpaEntity, Long> {
    List<SettlementShareJpaEntity> findAllBySettlementIdOrderById(Long settlementId);

    List<SettlementShareJpaEntity> findAllBySettlementIdInOrderById(List<Long> settlementIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SettlementShareJpaEntity share WHERE share.settlementId = :settlementId")
    void deleteAllBySettlementId(@Param("settlementId") Long settlementId);
}
