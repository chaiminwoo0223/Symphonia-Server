package com.symphonia.gathering.domain.repository;

import java.time.LocalDateTime;

// 참여자가 페어링 피드백을 냈다는 표시를 다룬다. 피드백 자체는 pairing이 저장한다.
public interface ParticipantPairingFeedbackRepository {
    // 참여자의 제출 표시가 없을 때만 남기고, 남겼으면 true를 반환한다.
    boolean markSubmittedIfAbsent(Long participantId, LocalDateTime submittedAt);

    boolean existsByParticipantId(Long participantId);

    void deleteAllByGatheringId(Long gatheringId);
}
