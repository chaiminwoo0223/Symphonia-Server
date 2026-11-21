package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.vo.CombinationId;
import com.symphonia.pairing.domain.vo.PairingRating;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PairingFeedbackJpaRepository
        extends JpaRepository<PairingFeedbackJpaEntity, Long> {
    // 같은 회원이 같은 조합의 첫 피드백을 동시에 내도 한 행만 생기도록, 이미 있으면 아무것도 하지 않는다.
    // 충돌한 쪽은 먼저 넣은 트랜잭션이 끝날 때까지 기다린 뒤 0을 반환하므로 트랜잭션이 깨지지 않는다.
    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO pairing_feedback
                        (member_id, drink_id, anju_id,
                         relationship_type, mood_type, rating, source, created_at, updated_at)
                    VALUES (:memberId, :drinkId, :anjuId,
                            :relationshipType, :moodType, :rating, :source, :now, :now)
                    ON CONFLICT (member_id, drink_id, anju_id) DO NOTHING
                    """,
            nativeQuery = true)
    int insertIfAbsent(
            @Param("memberId") Long memberId,
            @Param("drinkId") Long drinkId,
            @Param("anjuId") Long anjuId,
            @Param("relationshipType") String relationshipType,
            @Param("moodType") String moodType,
            @Param("rating") String rating,
            @Param("source") String source,
            @Param("now") LocalDateTime now);

    // member_id가 NULL인 행은 (member_id, drink_id, anju_id) 유니크 인덱스에서 서로 다른 값으로 보여 충돌하지 않는다.
    @Modifying
    @Query(
            """
            UPDATE PairingFeedbackJpaEntity feedback
            SET feedback.memberId = NULL
            WHERE feedback.memberId = :memberId
            """)
    void anonymizeByMemberId(@Param("memberId") Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT feedback FROM PairingFeedbackJpaEntity feedback
            WHERE feedback.memberId = :memberId
              AND feedback.drinkId = :drinkId
              AND feedback.anjuId = :anjuId
            """)
    Optional<PairingFeedbackJpaEntity> findByMemberIdAndCombinationForUpdate(
            @Param("memberId") Long memberId,
            @Param("drinkId") Long drinkId,
            @Param("anjuId") Long anjuId);

    // (member_id, drink_id, anju_id) 유니크 인덱스가 member_id로 시작해 그 인덱스로 읽는다.
    @Query(
            """
            SELECT new com.symphonia.pairing.domain.vo.CombinationId(feedback.drinkId, feedback.anjuId)
            FROM PairingFeedbackJpaEntity feedback
            WHERE feedback.memberId = :memberId
              AND feedback.rating = :rating
            """)
    List<CombinationId> findCombinationsByMemberIdAndRating(
            @Param("memberId") Long memberId, @Param("rating") PairingRating rating);
}
