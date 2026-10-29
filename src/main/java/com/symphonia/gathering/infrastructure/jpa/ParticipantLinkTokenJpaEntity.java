package com.symphonia.gathering.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "participant_link_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ParticipantLinkTokenJpaEntity {
    @Id private Long participantId;

    @Column(nullable = false, length = 64)
    private String tokenHash;

    public static ParticipantLinkTokenJpaEntity of(Long participantId, String tokenHash) {
        return new ParticipantLinkTokenJpaEntity(participantId, tokenHash);
    }
}
