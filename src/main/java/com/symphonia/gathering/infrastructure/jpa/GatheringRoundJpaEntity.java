package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.entity.GatheringRound;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 저장은 GatheringRoundJpaRepository.insertIfAbsent가 하므로 이 엔티티는 읽기에만 쓴다.
@Entity
@Table(name = "gathering_round")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GatheringRoundJpaEntity {
    @Id private Long id;

    @Column(nullable = false)
    private Long gatheringId;

    @Column(nullable = false)
    private int roundNumber;

    public GatheringRound toDomain() {
        return GatheringRound.builder().id(id).number(roundNumber).build();
    }
}
