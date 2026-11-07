package com.symphonia.gathering.domain.repository;

import java.time.LocalDateTime;

// 참여자가 페어링 피드백을 냈다는 표시를 다룬다. 피드백 자체는 pairing이 저장한다.
public interface ParticipantPairingFeedbackRepository {
    // 이미 표시가 있으면 PairingFeedbackAlreadySubmittedException을 던진다.
    void markSubmitted(Long participantId, LocalDateTime submittedAt);

    boolean existsByParticipantId(Long participantId);

    void deleteAllByGatheringId(Long gatheringId);
}
