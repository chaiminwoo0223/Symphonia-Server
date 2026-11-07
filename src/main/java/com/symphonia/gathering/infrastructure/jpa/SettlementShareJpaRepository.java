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

    // 정산 건 id를 따로 읽지 않고 서브쿼리로 넘겨, 정산 건 수와 관계없이 한 번에 지운다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            """
            DELETE FROM SettlementShareJpaEntity share
            WHERE share.settlementId IN (
                SELECT settlement.id FROM SettlementJpaEntity settlement
                WHERE settlement.gatheringId = :gatheringId)
            """)
    void deleteAllByGatheringId(@Param("gatheringId") Long gatheringId);
}
