package com.symphonia.gathering.infrastructure.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatheringPairingJpaRepository
        extends JpaRepository<GatheringPairingJpaEntity, Long> {
    // 모임장이 동시에 두 번 골라도 기본 키 위반 없이 나중 값으로 덮어쓴다.
    // 모임을 연 트랜잭션에서도 부르므로, 영속성 컨텍스트를 비워 방금 저장한 모임을 떼어 내지 않는다.
    @Modifying(flushAutomatically = true)
    @Query(
            value =
                    """
                    INSERT INTO gathering_pairing
                        (gathering_id, drink_id, anju_id, music_mood_id, relationship_type, mood_type)
                    VALUES (:gatheringId, :drinkId, :anjuId, :musicMoodId, :relationshipType, :moodType)
                    ON CONFLICT (gathering_id) DO UPDATE
                    SET drink_id = EXCLUDED.drink_id,
                        anju_id = EXCLUDED.anju_id,
                        music_mood_id = EXCLUDED.music_mood_id,
                        relationship_type = EXCLUDED.relationship_type,
                        mood_type = EXCLUDED.mood_type
                    """,
            nativeQuery = true)
    void upsert(
            @Param("gatheringId") Long gatheringId,
            @Param("drinkId") Long drinkId,
            @Param("anjuId") Long anjuId,
            @Param("musicMoodId") Long musicMoodId,
            @Param("relationshipType") String relationshipType,
            @Param("moodType") String moodType);

    @Query(
            """
            SELECT pairing FROM GatheringPairingJpaEntity pairing, ParticipantJpaEntity participant
            WHERE participant.id = :participantId
              AND pairing.gatheringId = participant.gatheringId
            """)
    Optional<GatheringPairingJpaEntity> findByParticipantId(
            @Param("participantId") Long participantId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM GatheringPairingJpaEntity pairing WHERE pairing.gatheringId = :gatheringId")
    void deleteByGatheringId(@Param("gatheringId") Long gatheringId);
}
