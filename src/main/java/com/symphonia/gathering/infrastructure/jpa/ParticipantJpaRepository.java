package com.symphonia.gathering.infrastructure.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantJpaRepository extends JpaRepository<ParticipantJpaEntity, Long> {
    List<ParticipantJpaEntity> findAllByGatheringIdOrderById(Long gatheringId);

    // 참조하는 행이 있으면 제약 위반으로 실패하므로, 부르는 쪽이 위반을 받아 처리한다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ParticipantJpaEntity participant WHERE participant.id = :participantId")
    void deleteParticipantById(@Param("participantId") Long participantId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "DELETE FROM ParticipantJpaEntity participant"
                    + " WHERE participant.gatheringId = :gatheringId")
    void deleteAllByGatheringId(@Param("gatheringId") Long gatheringId);
}
