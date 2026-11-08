package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.repository.ParticipantPairingFeedbackRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ParticipantPairingFeedbackRepositoryImpl
        implements ParticipantPairingFeedbackRepository {
    private final ParticipantPairingFeedbackJpaRepository participantPairingFeedbackJpaRepository;

    @Override
    public boolean markSubmittedIfAbsent(Long participantId, LocalDateTime submittedAt) {
        return participantPairingFeedbackJpaRepository.insertIfAbsent(participantId, submittedAt)
                == 1;
    }

    @Override
    public boolean existsByParticipantId(Long participantId) {
        return participantPairingFeedbackJpaRepository.existsById(participantId);
    }

    @Override
    public void deleteAllByGatheringId(Long gatheringId) {
        participantPairingFeedbackJpaRepository.deleteAllByGatheringId(gatheringId);
    }
}
