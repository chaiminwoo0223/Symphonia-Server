package com.symphonia.gathering.infrastructure.jpa;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantPairingFeedbackJpaRepository
        extends JpaRepository<ParticipantPairingFeedbackJpaEntity, Long> {
    // 같은 참여자가 동시에 내도 한 행만 생기도록, 이미 있으면 아무것도 하지 않는다.
    // 충돌한 쪽은 먼저 넣은 트랜잭션이 끝날 때까지 기다린 뒤 0을 반환하므로 트랜잭션이 깨지지 않는다.
    // 먼저 넣은 트랜잭션이 롤백되면 기다리던 쪽이 1을 반환하고 표시를 넣는다.
    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO participant_pairing_feedback (participant_id, submitted_at)
                    VALUES (:participantId, :submittedAt)
                    ON CONFLICT (participant_id) DO NOTHING
                    """,
            nativeQuery = true)
    int insertIfAbsent(
            @Param("participantId") Long participantId,
            @Param("submittedAt") LocalDateTime submittedAt);

    // 참여자 id를 따로 읽지 않고 서브쿼리로 넘겨, 참여자 수와 관계없이 한 번에 지운다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            """
            DELETE FROM ParticipantPairingFeedbackJpaEntity feedback
            WHERE feedback.participantId IN (
                SELECT participant.id FROM ParticipantJpaEntity participant
                WHERE participant.gatheringId = :gatheringId)
            """)
    void deleteAllByGatheringId(@Param("gatheringId") Long gatheringId);
}
