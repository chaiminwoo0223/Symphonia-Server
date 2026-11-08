package com.symphonia.gathering.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 제출 표시는 ParticipantPairingFeedbackJpaRepository.insertIfAbsent로만 넣으므로, 이 엔티티는 조회와 삭제에만 쓴다.
@Entity
@Table(name = "participant_pairing_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantPairingFeedbackJpaEntity {
    @Id private Long participantId;

    @Column(nullable = false)
    private LocalDateTime submittedAt;
}
