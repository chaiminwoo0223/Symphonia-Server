package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingRatingSummary;
import com.symphonia.pairing.domain.vo.RelationshipType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PairingRatingSummaryJpaRepository
        extends JpaRepository<
                PairingRatingSummaryJpaEntity, PairingRatingSummaryJpaEntity.Combination> {
    // 같은 조합의 피드백이 동시에 들어와도 행 잠금 안에서 더하므로 수를 잃지 않는다.
    // 음수 변화량은 평점을 바꿀 때만 나와 집계가 이미 있다.
    // 그래도 PostgreSQL은 충돌을 보기 전에 INSERT할 행의 CHECK 제약을 검사한다.
    // 그래서 INSERT 값은 0 아래로 내리지 않고, UPDATE에서 변화량을 더한다.
    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO pairing_rating_summary
                        (drink_id, anju_id, like_count, dislike_count)
                    VALUES (:drinkId, :anjuId, GREATEST(:likeDelta, 0), GREATEST(:dislikeDelta, 0))
                    ON CONFLICT (drink_id, anju_id) DO UPDATE
                    SET like_count = pairing_rating_summary.like_count + :likeDelta,
                        dislike_count = pairing_rating_summary.dislike_count + :dislikeDelta
                    """,
            nativeQuery = true)
    void accumulate(
            @Param("drinkId") Long drinkId,
            @Param("anjuId") Long anjuId,
            @Param("likeDelta") int likeDelta,
            @Param("dislikeDelta") int dislikeDelta);

    // 전역 집계에 요청한 상황의 집계를 LEFT JOIN해 한 번에 읽는다. 상황 집계가 없는 조합의 상황별 수는 0이다.
    // 엔티티를 거치지 않고 필요한 컬럼만 읽어 추천 요청마다 집계 전체를 읽는 비용을 줄인다.
    // 상황 조건이 모두 같음 비교라 상황 컬럼이 앞에 있는 유니크 인덱스로 요청한 상황의 행만 읽을 수 있다.
    @Query(
            """
            SELECT new com.symphonia.pairing.domain.vo.PairingRatingSummary(
                summary.combination.drinkId, summary.combination.anjuId,
                summary.likeCount, summary.dislikeCount,
                COALESCE(situation.likeCount, 0), COALESCE(situation.dislikeCount, 0))
            FROM PairingRatingSummaryJpaEntity summary
            LEFT JOIN PairingSituationRatingSummaryJpaEntity situation
              ON situation.drinkId = summary.combination.drinkId
             AND situation.anjuId = summary.combination.anjuId
             AND situation.relationshipType = :relationshipType
             AND situation.moodType = :moodType
            WHERE summary.likeCount + summary.dislikeCount >= :minimumSample
            """)
    List<PairingRatingSummary> findAllWithSampleAtLeastInSituation(
            @Param("minimumSample") int minimumSample,
            @Param("relationshipType") RelationshipType relationshipType,
            @Param("moodType") MoodType moodType);

    // 분위기 없이 추천받은 상황이다. NULL은 같음 비교로 맞지 않고, IS NOT DISTINCT FROM은 인덱스를 타지 못한다.
    @Query(
            """
            SELECT new com.symphonia.pairing.domain.vo.PairingRatingSummary(
                summary.combination.drinkId, summary.combination.anjuId,
                summary.likeCount, summary.dislikeCount,
                COALESCE(situation.likeCount, 0), COALESCE(situation.dislikeCount, 0))
            FROM PairingRatingSummaryJpaEntity summary
            LEFT JOIN PairingSituationRatingSummaryJpaEntity situation
              ON situation.drinkId = summary.combination.drinkId
             AND situation.anjuId = summary.combination.anjuId
             AND situation.relationshipType = :relationshipType
             AND situation.moodType IS NULL
            WHERE summary.likeCount + summary.dislikeCount >= :minimumSample
            """)
    List<PairingRatingSummary> findAllWithSampleAtLeastInSituationWithoutMood(
            @Param("minimumSample") int minimumSample,
            @Param("relationshipType") RelationshipType relationshipType);
}
