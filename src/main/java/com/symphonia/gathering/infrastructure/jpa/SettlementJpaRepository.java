package com.symphonia.gathering.infrastructure.jpa;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementJpaRepository extends JpaRepository<SettlementJpaEntity, Long> {
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
}
