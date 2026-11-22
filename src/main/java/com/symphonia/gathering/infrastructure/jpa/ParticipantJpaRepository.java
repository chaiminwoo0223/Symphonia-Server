package com.symphonia.gathering.infrastructure.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantJpaRepository extends JpaRepository<ParticipantJpaEntity, Long> {
    // 익명화한 참여자까지 모두 읽는다. 이름과 휴대폰이 비어 있으므로 도메인으로 변환하지 않고, 저장된 행을 확인하는 데만 쓴다.
    List<ParticipantJpaEntity> findAllByGatheringIdOrderById(Long gatheringId);

    // 철회하거나 제거돼 익명화한 참여자는 읽지 않는다.
    List<ParticipantJpaEntity> findAllByGatheringIdAndWithdrawnAtIsNullOrderById(Long gatheringId);

    boolean existsByGatheringIdAndGoogleSubHmac(Long gatheringId, String googleSubHmac);

    @Query(
            "SELECT participant.id FROM ParticipantJpaEntity participant"
                    + " WHERE participant.gatheringId = :gatheringId"
                    + " AND participant.googleSubHmac = :googleSubHmac")
    Optional<Long> findIdByGatheringIdAndGoogleSubHmac(
            @Param("gatheringId") Long gatheringId, @Param("googleSubHmac") String googleSubHmac);

    // 같은 모임에 같은 휴대폰이 이미 있으면 유일 제약 위반으로 실패한다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "UPDATE ParticipantJpaEntity participant"
                    + " SET participant.encryptedName = :encryptedName,"
                    + " participant.encryptedPhoneNumber = :encryptedPhoneNumber,"
                    + " participant.phoneNumberHmac = :phoneNumberHmac"
                    + " WHERE participant.id = :participantId")
    void updateNameAndPhoneNumberById(
            @Param("participantId") Long participantId,
            @Param("encryptedName") byte[] encryptedName,
            @Param("encryptedPhoneNumber") byte[] encryptedPhoneNumber,
            @Param("phoneNumberHmac") String phoneNumberHmac);

    // 이름, 휴대폰, 구글 계정 값을 비우고 철회 시각만 남긴다. 정산 건이 참조하는 행은 지울 수 없을 때 쓴다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "UPDATE ParticipantJpaEntity participant"
                    + " SET participant.encryptedName = NULL,"
                    + " participant.encryptedPhoneNumber = NULL,"
                    + " participant.phoneNumberHmac = NULL,"
                    + " participant.googleSubHmac = NULL,"
                    + " participant.withdrawnAt = :withdrawnAt"
                    + " WHERE participant.id = :participantId")
    void anonymizeParticipantById(
            @Param("participantId") Long participantId,
            @Param("withdrawnAt") LocalDateTime withdrawnAt);

    // 같은 모임에 같은 값이 이미 있으면 유일 제약 위반으로 실패한다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "UPDATE ParticipantJpaEntity participant"
                    + " SET participant.googleSubHmac = :googleSubHmac"
                    + " WHERE participant.id = :participantId")
    void updateGoogleSubHmac(
            @Param("participantId") Long participantId,
            @Param("googleSubHmac") String googleSubHmac);

    // 참조하는 행이 있으면 제약 위반으로 실패하므로, 부르는 쪽이 참조하는 행을 먼저 지운다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ParticipantJpaEntity participant WHERE participant.id = :participantId")
    void deleteParticipantById(@Param("participantId") Long participantId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "DELETE FROM ParticipantJpaEntity participant"
                    + " WHERE participant.gatheringId = :gatheringId")
    void deleteAllByGatheringId(@Param("gatheringId") Long gatheringId);
}
