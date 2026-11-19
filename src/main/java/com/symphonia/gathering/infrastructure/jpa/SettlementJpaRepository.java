package com.symphonia.gathering.infrastructure.jpa;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementJpaRepository extends JpaRepository<SettlementJpaEntity, Long> {
    // 같은 결제자와 요청 id의 정산 건이 이미 있으면 넣지 않고 빈 값을 돌려준다. 동시에 같은 요청이 오면 늦은 요청은 먼저 넣은 요청이 커밋한 뒤에 빈 값을 받는다.
    // 감사 시각 컬럼은 기록 시각으로 채운다. 이 쿼리는 JPA 엔티티를 거치지 않아 감사 리스너가 값을 넣지 못한다.
    @Query(
            value =
                    """
                    INSERT INTO settlement
                        (gathering_id, round_id, payer_participant_id, total_amount, recorded_at,
                         client_request_id, version, created_at, updated_at)
                    VALUES (:gatheringId, :roundId, :payerParticipantId, :totalAmount, :recordedAt,
                            :clientRequestId, 0, :recordedAt, :recordedAt)
                    ON CONFLICT (payer_participant_id, client_request_id) DO NOTHING
                    RETURNING id
                    """,
            nativeQuery = true)
    Optional<Long> insertIfAbsent(
            @Param("gatheringId") Long gatheringId,
            @Param("roundId") Long roundId,
            @Param("payerParticipantId") Long payerParticipantId,
            @Param("totalAmount") long totalAmount,
            @Param("recordedAt") LocalDateTime recordedAt,
            @Param("clientRequestId") UUID clientRequestId);

    Optional<SettlementJpaEntity> findByPayerParticipantIdAndClientRequestId(
            Long payerParticipantId, UUID clientRequestId);

    boolean existsByPayerParticipantId(Long payerParticipantId);

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

    List<SettlementJpaEntity> findAllByGatheringIdOrderById(Long gatheringId);

    List<SettlementJpaEntity> findAllByGatheringIdInOrderById(Collection<Long> gatheringIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT settlement FROM SettlementJpaEntity settlement
            WHERE settlement.gatheringId = :gatheringId
            ORDER BY settlement.id
            """)
    List<SettlementJpaEntity> findAllByGatheringIdForUpdate(@Param("gatheringId") Long gatheringId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "DELETE FROM SettlementJpaEntity settlement"
                    + " WHERE settlement.gatheringId = :gatheringId")
    void deleteAllByGatheringId(@Param("gatheringId") Long gatheringId);
}
