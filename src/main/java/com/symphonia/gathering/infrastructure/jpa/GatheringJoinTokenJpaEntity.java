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
@Table(name = "gathering_join_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GatheringJoinTokenJpaEntity {
    @Id private Long gatheringId;

    @Column(nullable = false, length = 64)
    private String tokenHash;

    public static GatheringJoinTokenJpaEntity of(Long gatheringId, String tokenHash) {
        return new GatheringJoinTokenJpaEntity(gatheringId, tokenHash);
    }
}
