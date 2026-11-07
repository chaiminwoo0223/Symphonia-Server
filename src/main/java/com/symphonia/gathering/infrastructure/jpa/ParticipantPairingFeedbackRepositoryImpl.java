package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.ParticipantPairingFeedbackRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ParticipantPairingFeedbackRepositoryImpl
        implements ParticipantPairingFeedbackRepository {
    private final ParticipantPairingFeedbackJpaRepository participantPairingFeedbackJpaRepository;

    // 같은 참여자가 동시에 내면 Service의 중복 검사를 둘 다 통과하므로 기본 키 위반으로 구분한다.
    // 나중 요청은 먼저 넣은 트랜잭션이 끝날 때까지 기다리므로, 먼저 낸 피드백이 실패해 롤백되면 나중 요청이 저장된다.
    @Override
    public void markSubmitted(Long participantId, LocalDateTime submittedAt) {
        try {
            participantPairingFeedbackJpaRepository.saveAndFlush(
                    ParticipantPairingFeedbackJpaEntity.of(participantId, submittedAt));
        } catch (DataIntegrityViolationException e) {
            GatheringPolicy.validatePairingFeedbackNotSubmitted(isPrimaryKeyViolation(e));
            throw e;
        }
    }

    @Override
    public boolean existsByParticipantId(Long participantId) {
        return participantPairingFeedbackJpaRepository.existsById(participantId);
    }

    @Override
    public void deleteAllByGatheringId(Long gatheringId) {
        participantPairingFeedbackJpaRepository.deleteAllByGatheringId(gatheringId);
    }

    private boolean isPrimaryKeyViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException violation
                && "pk_participant_pairing_feedback"
                        .equalsIgnoreCase(violation.getConstraintName());
    }
}
