package com.symphonia.gathering.infrastructure.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatheringRoundJpaRepository extends JpaRepository<GatheringRoundJpaEntity, Long> {
    // 같은 번호를 동시에 추가해도 한 행만 생기도록, 이미 있으면 아무것도 하지 않는다.
    // 충돌한 쪽은 먼저 넣은 트랜잭션이 끝날 때까지 기다린 뒤 넘어가므로 트랜잭션이 깨지지 않는다.
    // 모임을 연 트랜잭션에서도 부르므로, 영속성 컨텍스트를 비워 방금 저장한 모임을 떼어 내지 않는다.
    @Modifying(flushAutomatically = true)
    @Query(
            value =
                    """
                    INSERT INTO gathering_round (gathering_id, round_number)
                    VALUES (:gatheringId, :roundNumber)
                    ON CONFLICT (gathering_id, round_number) DO NOTHING
                    """,
            nativeQuery = true)
    void insertIfAbsent(
            @Param("gatheringId") Long gatheringId, @Param("roundNumber") int roundNumber);

    List<GatheringRoundJpaEntity> findAllByGatheringIdOrderByRoundNumber(Long gatheringId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "DELETE FROM GatheringRoundJpaEntity gatheringRound WHERE gatheringRound.gatheringId = :gatheringId")
    void deleteAllByGatheringId(@Param("gatheringId") Long gatheringId);
}
