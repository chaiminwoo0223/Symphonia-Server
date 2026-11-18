package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.common.entity.BaseTimeEntity;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.entity.SettlementShare;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "settlement")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gatheringId;

    @Column(nullable = false)
    private Long roundId;

    @Column(nullable = false)
    private Long payerParticipantId;

    @Column(nullable = false)
    private long totalAmount;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    private LocalDateTime cancelledAt;

    private UUID clientRequestId;

    @Version private Long version;

    public static SettlementJpaEntity from(Settlement settlement) {
        return SettlementJpaEntity.builder()
                .id(settlement.getId())
                .gatheringId(settlement.getGatheringId())
                .roundId(settlement.getRoundId())
                .payerParticipantId(settlement.getPayerParticipantId())
                .totalAmount(settlement.getTotalAmount())
                .recordedAt(settlement.getRecordedAt())
                .cancelledAt(settlement.getCancelledAt())
                .clientRequestId(settlement.getClientRequestId())
                .version(settlement.getVersion())
                .build();
    }

    // 분담금은 별도 테이블에서 읽어 받는다.
    public Settlement toDomain(List<SettlementShare> shares) {
        return Settlement.builder()
                .id(id)
                .gatheringId(gatheringId)
                .roundId(roundId)
                .payerParticipantId(payerParticipantId)
                .totalAmount(totalAmount)
                .shares(shares)
                .recordedAt(recordedAt)
                .cancelledAt(cancelledAt)
                .clientRequestId(clientRequestId)
                .version(version)
                .build();
    }
}
