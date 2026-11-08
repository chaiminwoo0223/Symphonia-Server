package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.vo.GatheringPairing;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 저장은 GatheringPairingJpaRepository.upsert가 하므로 이 엔티티는 읽기에만 쓴다.
@Entity
@Table(name = "gathering_pairing")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GatheringPairingJpaEntity {
    @Id private Long gatheringId;

    @Column(nullable = false)
    private Long drinkId;

    @Column(nullable = false)
    private Long anjuId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationshipType relationshipType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MoodType moodType;

    public GatheringPairing toDomain() {
        return GatheringPairing.of(drinkId, anjuId, relationshipType, moodType);
    }
}
