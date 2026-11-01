package com.symphonia.notification.infrastructure.jpa;

import com.symphonia.notification.domain.entity.NotificationSchedule;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationScheduleJpaEntity {
    @Id private Long gatheringId;

    private LocalDateTime scheduledAt;

    public static NotificationScheduleJpaEntity from(NotificationSchedule notificationSchedule) {
        return new NotificationScheduleJpaEntity(
                notificationSchedule.getGatheringId(), notificationSchedule.getScheduledAt());
    }

    public NotificationSchedule toDomain() {
        return NotificationSchedule.builder()
                .gatheringId(gatheringId)
                .scheduledAt(scheduledAt)
                .build();
    }
}
