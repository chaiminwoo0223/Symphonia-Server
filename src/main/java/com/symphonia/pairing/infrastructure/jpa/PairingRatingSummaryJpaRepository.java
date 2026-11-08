package com.symphonia.pairing.infrastructure.jpa;

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

    @Query(
            """
            SELECT summary FROM PairingRatingSummaryJpaEntity summary
            WHERE summary.likeCount + summary.dislikeCount >= :minimumSample
            """)
    List<PairingRatingSummaryJpaEntity> findAllWithSampleAtLeast(
            @Param("minimumSample") int minimumSample);
}
