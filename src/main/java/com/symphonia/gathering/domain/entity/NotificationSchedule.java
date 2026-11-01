package com.symphonia.gathering.domain.entity;

import com.symphonia.gathering.domain.policy.NotificationPolicy;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 모임의 정산 메시지 발송 예약이다. 모임마다 하나만 두고, 보낼 정산 건이 없으면 예약 시각을 비워 둔다.
// 정산 건을 기록하거나 수정할 때마다 마지막 기록 시각으로 예약 시각을 다시 계산한다.
// 그래서 이어서 기록한 1차와 2차 정산 건은 한 메시지로 묶이고, 모든 정산 건이 발송 전에 수정 시간을 받는다.
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationSchedule {
    private Long gatheringId;
    // 보낼 정산 건이 있을 때만 값이 있다.
    private LocalDateTime scheduledAt;

    public static NotificationSchedule empty(Long gatheringId) {
        return NotificationSchedule.builder().gatheringId(gatheringId).build();
    }

    // 발송 뒤에 기록한 정산 건도 같은 방식으로 새 예약을 잡는다. 발송하면 예약 시각을 비우기 때문이다.
    public void reschedule(LocalDateTime recordedAt) {
        scheduledAt = NotificationPolicy.scheduledAt(recordedAt);
    }

    // 장애나 지연으로 보낼 수 없는 시간대에 발송을 시작하게 되면 그다음 10:00로 미룬다.
    // 예약 시각을 바꿔 두므로 결제자 화면에도 바뀐 시각이 보이고, 그때까지 정산 건을 고칠 수 있다.
    public void defer(LocalDateTime now) {
        scheduledAt = NotificationPolicy.nextSendingStart(now);
    }

    public void clear() {
        scheduledAt = null;
    }

    public boolean isScheduled() {
        return scheduledAt != null;
    }
}
