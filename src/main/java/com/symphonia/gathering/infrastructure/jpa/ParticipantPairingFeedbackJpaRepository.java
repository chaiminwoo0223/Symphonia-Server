package com.symphonia.gathering.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantPairingFeedbackJpaRepository
        extends JpaRepository<ParticipantPairingFeedbackJpaEntity, Long> {
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
