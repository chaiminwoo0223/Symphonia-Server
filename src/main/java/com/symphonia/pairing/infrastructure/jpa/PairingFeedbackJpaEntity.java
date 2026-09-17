package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.common.entity.BaseTimeEntity;
import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PairingFeedbackJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long drinkId;

    @Column(nullable = false)
    private Long anjuId;

    @Column(nullable = false)
    private Long musicMoodId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationshipType relationshipType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MoodType moodType;

    public static PairingFeedbackJpaEntity from(PairingFeedback pairingFeedback) {
        return PairingFeedbackJpaEntity.builder()
                .id(pairingFeedback.getId())
                .memberId(pairingFeedback.getMemberId())
                .drinkId(pairingFeedback.getDrinkId())
                .anjuId(pairingFeedback.getAnjuId())
                .musicMoodId(pairingFeedback.getMusicMoodId())
                .relationshipType(pairingFeedback.getRelationshipType())
                .moodType(pairingFeedback.getMoodType())
                .build();
    }

    public PairingFeedback toDomain() {
        return PairingFeedback.builder()
                .id(id)
                .memberId(memberId)
                .drinkId(drinkId)
                .anjuId(anjuId)
                .musicMoodId(musicMoodId)
                .relationshipType(relationshipType)
                .moodType(moodType)
                .build();
    }
}
