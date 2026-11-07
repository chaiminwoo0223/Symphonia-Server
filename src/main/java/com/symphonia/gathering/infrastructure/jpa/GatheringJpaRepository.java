package com.symphonia.gathering.infrastructure.jpa;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatheringJpaRepository extends JpaRepository<GatheringJpaEntity, Long> {
    @Query(
            """
            SELECT gathering.id FROM GatheringJpaEntity gathering
            WHERE gathering.openedAt <= :openedAt
            ORDER BY gathering.id
            """)
    List<Long> findIdsOpenedUntil(@Param("openedAt") LocalDateTime openedAt);

    // 다른 서버가 파기 중인 모임은 기다리지 않고 건너뛴다. 잠그는 순서가 서버마다 같도록 id 순으로 읽는다.
    @Query(
            value =
                    """
                    SELECT * FROM gathering
                    WHERE id IN (:ids)
                    ORDER BY id
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true)
    List<GatheringJpaEntity> findAllByIdsForUpdateSkipLocked(@Param("ids") Collection<Long> ids);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM GatheringJpaEntity gathering WHERE gathering.id IN :ids")
    void deleteAllByIds(@Param("ids") Collection<Long> ids);
}
