package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.vo.PairingRatingSummary;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

// 갱신은 PairingRatingSummaryJpaRepository의 UPSERT로만 하므로 읽기에만 쓴다.
@Entity
@Immutable
@Table(name = "pairing_rating_summary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PairingRatingSummaryJpaEntity {
    @EmbeddedId private Combination combination;

    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private int dislikeCount;

    public PairingRatingSummary toDomain() {
        return PairingRatingSummary.of(
                combination.drinkId(), combination.anjuId(), likeCount, dislikeCount);
    }

    @Embeddable
    public record Combination(Long drinkId, Long anjuId) implements Serializable {}
}
