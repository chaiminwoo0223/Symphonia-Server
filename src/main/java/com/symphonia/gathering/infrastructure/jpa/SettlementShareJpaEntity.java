package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.entity.SettlementShare;
import com.symphonia.gathering.domain.vo.ShareStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "settlement_share")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SettlementShareJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long settlementId;

    @Column(nullable = false)
    private Long participantId;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private boolean fixed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShareStatus status;

    public static SettlementShareJpaEntity from(SettlementShare share, Long settlementId) {
        return new SettlementShareJpaEntity(
                null,
                settlementId,
                share.getParticipantId(),
                share.getAmount(),
                share.isFixed(),
                share.getStatus());
    }

    public SettlementShare toDomain() {
        return SettlementShare.builder()
                .participantId(participantId)
                .amount(amount)
                .fixed(fixed)
                .status(status)
                .build();
    }
}
