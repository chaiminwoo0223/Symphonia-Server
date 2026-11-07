package com.symphonia.gathering.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "participant_pairing_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ParticipantPairingFeedbackJpaEntity implements Persistable<Long> {
    @Id private Long participantId;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    public static ParticipantPairingFeedbackJpaEntity of(
            Long participantId, LocalDateTime submittedAt) {
        return new ParticipantPairingFeedbackJpaEntity(participantId, submittedAt);
    }

    @Override
    public Long getId() {
        return participantId;
    }

    // 피드백은 한 번만 내므로 항상 새 행으로 보고 INSERT한다. 같은 참여자의 행이 있으면 기본 키 위반이 난다.
    @Override
    public boolean isNew() {
        return true;
    }
}
