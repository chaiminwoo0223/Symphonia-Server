package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.vo.MoodType;
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
import org.hibernate.annotations.Immutable;

// 갱신은 PairingSelectionSummaryJpaRepository의 UPSERT로만 하므로 읽기에만 쓴다.
@Entity
@Immutable
@Table(name = "pairing_selection_summary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PairingSelectionSummaryJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationshipType relationshipType;

    // 분위기 없이 고른 조합이면 null이다.
    @Enumerated(EnumType.STRING)
    private MoodType moodType;

    @Column(nullable = false)
    private Long drinkId;

    @Column(nullable = false)
    private Long anjuId;

    @Column(nullable = false)
    private int selectionCount;
}
