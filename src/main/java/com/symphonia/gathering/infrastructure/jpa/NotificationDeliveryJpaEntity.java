package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.common.entity.BaseTimeEntity;
import com.symphonia.gathering.domain.entity.NotificationDelivery;
import com.symphonia.gathering.domain.vo.DeliveryStatus;
import com.symphonia.gathering.domain.vo.NotificationType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notification_delivery")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDeliveryJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gatheringId;

    @Column(nullable = false)
    private Long participantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    // 메시지에 담을 정산 건 목록은 발송 기록을 만든 뒤 바뀌지 않는다.
    // Set으로 두어 상태만 바꿔 저장할 때 Hibernate가 바뀐 원소만 반영한다. List는 저장할 때마다 행을 모두 지우고 다시 넣는다.
    @ElementCollection
    @CollectionTable(
            name = "notification_delivery_settlement",
            joinColumns = @JoinColumn(name = "delivery_id"))
    @Column(name = "settlement_id", nullable = false)
    private Set<Long> settlementIds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private LocalDateTime availableAt;

    private LocalDateTime publishedAt;

    private LocalDateTime claimedAt;

    private LocalDateTime sentAt;

    public static NotificationDeliveryJpaEntity from(NotificationDelivery notificationDelivery) {
        return NotificationDeliveryJpaEntity.builder()
                .id(notificationDelivery.getId())
                .gatheringId(notificationDelivery.getGatheringId())
                .participantId(notificationDelivery.getParticipantId())
                .type(notificationDelivery.getType())
                .settlementIds(new HashSet<>(notificationDelivery.getSettlementIds()))
                .status(notificationDelivery.getStatus())
                .attempts(notificationDelivery.getAttempts())
                .availableAt(notificationDelivery.getAvailableAt())
                .publishedAt(notificationDelivery.getPublishedAt())
                .claimedAt(notificationDelivery.getClaimedAt())
                .sentAt(notificationDelivery.getSentAt())
                .build();
    }

    public NotificationDelivery toDomain() {
        return NotificationDelivery.builder()
                .id(id)
                .gatheringId(gatheringId)
                .participantId(participantId)
                .type(type)
                .settlementIds(settlementIds.stream().sorted().toList())
                .status(status)
                .attempts(attempts)
                .availableAt(availableAt)
                .publishedAt(publishedAt)
                .claimedAt(claimedAt)
                .sentAt(sentAt)
                .build();
    }
}
