package com.symphonia.gathering.infrastructure.jpa;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantLinkTokenJpaRepository
        extends JpaRepository<ParticipantLinkTokenJpaEntity, Long> {
    Optional<ParticipantLinkTokenJpaEntity> findByTokenHash(String tokenHash);

    // 참여자 id를 따로 읽지 않고 서브쿼리로 넘겨, 모임 수와 관계없이 한 번에 지운다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            """
            DELETE FROM ParticipantLinkTokenJpaEntity token
            WHERE token.participantId IN (
                SELECT participant.id FROM ParticipantJpaEntity participant
                WHERE participant.gatheringId IN :gatheringIds)
            """)
    void deleteAllByGatheringIds(@Param("gatheringIds") Collection<Long> gatheringIds);
}
