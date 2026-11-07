package com.symphonia.gathering.infrastructure.jpa;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementJpaRepository extends JpaRepository<SettlementJpaEntity, Long> {
    List<SettlementJpaEntity> findAllByIdInOrderById(Collection<Long> ids);

    Optional<SettlementJpaEntity> findFirstByGatheringIdAndCancelledAtIsNullOrderByIdDesc(
            Long gatheringId);

    List<SettlementJpaEntity> findAllByPayerParticipantIdAndCancelledAtIsNullOrderById(
            Long payerParticipantId);

    @Query(
            """
            SELECT settlement FROM SettlementJpaEntity settlement
            WHERE settlement.cancelledAt IS NULL
              AND settlement.id IN (
                  SELECT share.settlementId FROM SettlementShareJpaEntity share
                  WHERE share.participantId = :participantId)
            ORDER BY settlement.id
            """)
    List<SettlementJpaEntity> findAllByShareParticipantId(
            @Param("participantId") Long participantId);

    // 잠그는 순서가 요청마다 같도록 id 순으로 읽는다. 순서가 다르면 두 요청이 서로를 기다릴 수 있다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT settlement FROM SettlementJpaEntity settlement
            WHERE settlement.cancelledAt IS NULL
              AND settlement.payerParticipantId = :payerParticipantId
              AND settlement.id IN (
                  SELECT share.settlementId FROM SettlementShareJpaEntity share
                  WHERE share.participantId = :participantId)
            ORDER BY settlement.id
            """)
    List<SettlementJpaEntity> findAllByShareParticipantIdAndPayerForUpdate(
            @Param("participantId") Long participantId,
            @Param("payerParticipantId") Long payerParticipantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT settlement FROM SettlementJpaEntity settlement
            WHERE settlement.gatheringId = :gatheringId
              AND settlement.notifiedAt IS NULL
              AND settlement.cancelledAt IS NULL
            ORDER BY settlement.id
            """)
    List<SettlementJpaEntity> findAllUnnotifiedByGatheringIdForUpdate(
            @Param("gatheringId") Long gatheringId);

    List<SettlementJpaEntity> findAllByGatheringIdOrderById(Long gatheringId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT settlement FROM SettlementJpaEntity settlement
            WHERE settlement.gatheringId IN :gatheringIds
            ORDER BY settlement.id
            """)
    List<SettlementJpaEntity> findAllByGatheringIdsForUpdate(
            @Param("gatheringIds") Collection<Long> gatheringIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "DELETE FROM SettlementJpaEntity settlement"
                    + " WHERE settlement.gatheringId IN :gatheringIds")
    void deleteAllByGatheringIds(@Param("gatheringIds") Collection<Long> gatheringIds);
}
