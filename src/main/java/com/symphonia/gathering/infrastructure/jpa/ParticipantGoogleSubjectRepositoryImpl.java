package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.ParticipantGoogleSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ParticipantGoogleSubjectRepositoryImpl implements ParticipantGoogleSubjectRepository {
    // 휴대폰 HMAC과 같은 모임별 키를 쓰므로, 두 값이 섞여 일치하지 않도록 구글 계정 값에는 접두사를 붙여 계산한다.
    private static final String HMAC_INPUT_PREFIX = "google:";

    private final ParticipantJpaRepository participantJpaRepository;
    private final GatheringDataKeyLoader gatheringDataKeyLoader;

    @Override
    public boolean exists(Long gatheringId, String googleSubject) {
        return participantJpaRepository.existsByGatheringIdAndGoogleSubHmac(
                gatheringId, hmac(gatheringId, googleSubject));
    }

    @Override
    public void save(Long gatheringId, Long participantId, String googleSubject) {
        try {
            participantJpaRepository.updateGoogleSubHmac(
                    participantId, hmac(gatheringId, googleSubject));
        } catch (DataIntegrityViolationException e) {
            GatheringPolicy.validateNotJoined(isGoogleSubViolation(e));
            throw e;
        }
    }

    private String hmac(Long gatheringId, String googleSubject) {
        return gatheringDataKeyLoader.load(gatheringId).hmac(HMAC_INPUT_PREFIX + googleSubject);
    }

    // 같은 구글 계정으로 동시에 참여하면 Service의 중복 검사를 둘 다 통과하므로 unique 제약 위반으로 구분한다.
    private boolean isGoogleSubViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException violation
                && "uk_participant_google_sub".equalsIgnoreCase(violation.getConstraintName());
    }
}
