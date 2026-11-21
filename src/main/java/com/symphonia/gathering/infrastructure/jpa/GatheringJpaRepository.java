package com.symphonia.gathering.infrastructure.jpa;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatheringJpaRepository extends JpaRepository<GatheringJpaEntity, Long> {
    List<GatheringJpaEntity> findAllByOpenedAtLessThanEqualOrderById(LocalDateTime openedAt);

    // 다른 요청이 잠근 모임은 그 요청이 끝날 때까지 기다린다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT gathering FROM GatheringJpaEntity gathering WHERE gathering.id = :id")
    Optional<GatheringJpaEntity> findByIdForUpdate(@Param("id") Long id);

    // 다른 서버가 파기 중인 모임은 기다리지 않고 건너뛴다.
    @Query(
            value = "SELECT * FROM gathering WHERE id = :id FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    Optional<GatheringJpaEntity> findByIdForUpdateSkipLocked(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "UPDATE GatheringJpaEntity gathering SET gathering.recordingExtended = true WHERE gathering.id = :id")
    void markRecordingExtended(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM GatheringJpaEntity gathering WHERE gathering.id = :id")
    void deleteGathering(@Param("id") Long id);
}
