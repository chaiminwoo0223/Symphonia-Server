package com.symphonia.pairing.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PairingSituationRatingSummaryJpaRepository
        extends JpaRepository<PairingSituationRatingSummaryJpaEntity, Long> {
    // 전역 집계와 같은 방식으로 행 잠금 안에서 더하고, INSERT 값은 0 아래로 내리지 않는다.
    // 분위기가 null인 상황도 하나의 행이라 NULLS NOT DISTINCT 제약으로 충돌을 잡는다.
    // 분위기를 null로 바인딩하면 PostgreSQL이 타입을 알 수 없어 VARCHAR로 형변환한다.
    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO pairing_situation_rating_summary
                        (relationship_type, mood_type, drink_id, anju_id, like_count, dislike_count)
                    VALUES (:relationshipType, CAST(:moodType AS VARCHAR), :drinkId, :anjuId,
                            GREATEST(:likeDelta, 0), GREATEST(:dislikeDelta, 0))
                    ON CONFLICT ON CONSTRAINT uk_pairing_situation_rating_summary DO UPDATE
                    SET like_count = pairing_situation_rating_summary.like_count + :likeDelta,
                        dislike_count = pairing_situation_rating_summary.dislike_count + :dislikeDelta
                    """,
            nativeQuery = true)
    void accumulate(
            @Param("relationshipType") String relationshipType,
            @Param("moodType") String moodType,
            @Param("drinkId") Long drinkId,
            @Param("anjuId") Long anjuId,
            @Param("likeDelta") int likeDelta,
            @Param("dislikeDelta") int dislikeDelta);
}
