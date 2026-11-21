package com.symphonia.pairing.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PairingSelectionSummaryJpaRepository
        extends JpaRepository<PairingSelectionSummaryJpaEntity, Long> {
    // 같은 상황의 조합을 담은 모임을 동시에 파기해도 행 잠금 안에서 더해 횟수가 유실되지 않는다.
    // 분위기가 null인 상황도 하나의 행이라 NULLS NOT DISTINCT 제약으로 충돌을 잡는다.
    // 분위기를 null로 바인딩하면 PostgreSQL이 타입을 알 수 없어 VARCHAR로 형변환한다.
    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO pairing_selection_summary
                        (relationship_type, mood_type, drink_id, anju_id, selection_count)
                    VALUES (:relationshipType, CAST(:moodType AS VARCHAR), :drinkId, :anjuId, 1)
                    ON CONFLICT ON CONSTRAINT uk_pairing_selection_summary DO UPDATE
                    SET selection_count = pairing_selection_summary.selection_count + 1
                    """,
            nativeQuery = true)
    void accumulate(
            @Param("relationshipType") String relationshipType,
            @Param("moodType") String moodType,
            @Param("drinkId") Long drinkId,
            @Param("anjuId") Long anjuId);
}
