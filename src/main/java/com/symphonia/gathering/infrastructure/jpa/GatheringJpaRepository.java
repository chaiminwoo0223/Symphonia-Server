package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.vo.HostedGathering;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatheringJpaRepository extends JpaRepository<GatheringJpaEntity, Long> {
    // 같은 모임장과 요청 id의 모임이 이미 있으면 넣지 않고 빈 값을 돌려준다. 동시에 같은 요청이 오면 늦은 요청은 먼저 넣은 요청이 커밋한 뒤에 빈 값을 받는다.
    // 감사 시각 컬럼은 연 시각으로 채운다. 이 쿼리는 JPA 엔티티를 거치지 않아 감사 리스너가 값을 넣지 못한다.
    @Query(
            value =
                    """
                    INSERT INTO gathering
                        (host_member_id, name, opened_at, recording_extended, client_request_id,
                         created_at, updated_at)
                    VALUES (:hostMemberId, :name, :openedAt, false, :clientRequestId,
                            :openedAt, :openedAt)
                    ON CONFLICT (host_member_id, client_request_id) DO NOTHING
                    RETURNING id
                    """,
            nativeQuery = true)
    Optional<Long> insertIfAbsent(
            @Param("hostMemberId") Long hostMemberId,
            @Param("name") String name,
            @Param("openedAt") LocalDateTime openedAt,
            @Param("clientRequestId") UUID clientRequestId);

    Optional<GatheringJpaEntity> findByHostMemberIdAndClientRequestId(
            Long hostMemberId, UUID clientRequestId);

    List<GatheringJpaEntity> findAllByOpenedAtLessThanEqualOrderById(LocalDateTime openedAt);

    List<GatheringJpaEntity> findAllByHostMemberIdOrderById(Long hostMemberId);

    // 참여자는 복호화하지 않고 수만 센다.
    @Query(
            """
            SELECT new com.symphonia.gathering.domain.vo.HostedGathering(
                gathering.id,
                gathering.name,
                (SELECT COUNT(participant) FROM ParticipantJpaEntity participant
                    WHERE participant.gatheringId = gathering.id
                      AND participant.withdrawnAt IS NULL))
            FROM GatheringJpaEntity gathering
            WHERE gathering.id IN :ids
            ORDER BY gathering.id DESC
            """)
    List<HostedGathering> findAllHostedByIds(@Param("ids") List<Long> ids);

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
            "UPDATE GatheringJpaEntity gathering SET gathering.name = :name WHERE gathering.id = :id")
    void updateName(@Param("id") Long id, @Param("name") String name);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "UPDATE GatheringJpaEntity gathering SET gathering.recordingExtended = true WHERE gathering.id = :id")
    void markRecordingExtended(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM GatheringJpaEntity gathering WHERE gathering.id = :id")
    void deleteGathering(@Param("id") Long id);
}
