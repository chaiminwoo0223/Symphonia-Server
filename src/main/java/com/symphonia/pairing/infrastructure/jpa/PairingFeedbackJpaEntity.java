package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.common.entity.BaseTimeEntity;
import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingRating;
import com.symphonia.pairing.domain.vo.RelationshipType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "pairing_feedback")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PairingFeedbackJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 익명 피드백(#131)은 회원 id가 없다.
    private Long memberId;

    @Column(nullable = false)
    private Long drinkId;

    @Column(nullable = false)
    private Long anjuId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationshipType relationshipType;

    // 분위기 없이 추천받은 조합이면 null이다(#184).
    @Enumerated(EnumType.STRING)
    private MoodType moodType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PairingRating rating;

    public static PairingFeedbackJpaEntity from(PairingFeedback pairingFeedback) {
        return PairingFeedbackJpaEntity.builder()
                .id(pairingFeedback.getId())
                .memberId(pairingFeedback.getMemberId())
                .drinkId(pairingFeedback.getDrinkId())
                .anjuId(pairingFeedback.getAnjuId())
                .relationshipType(pairingFeedback.getRelationshipType())
                .moodType(pairingFeedback.getMoodType())
                .rating(pairingFeedback.getRating())
                .build();
    }

    public PairingFeedback toDomain() {
        return PairingFeedback.builder()
                .id(id)
                .memberId(memberId)
                .drinkId(drinkId)
                .anjuId(anjuId)
                .relationshipType(relationshipType)
                .moodType(moodType)
                .rating(rating)
                .build();
    }
}
