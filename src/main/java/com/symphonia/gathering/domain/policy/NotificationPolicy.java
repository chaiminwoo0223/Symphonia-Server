package com.symphonia.gathering.domain.policy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

// 늦은 밤이나 새벽에 돈 이야기가 담긴 메시지를 받지 않게, 정산 메시지와 알림을 보낼 수 있는 시간대를 정한다.
// 시각은 모두 한국 시간이다.
public enum NotificationPolicy {
    ;

    // 결제자가 발송 전에 기록을 고칠 수 있는 시간이다. 모든 정산 건이 발송 전에 이만큼의 수정 시간을 받는다.
    private static final Duration EDIT_WINDOW = Duration.ofMinutes(30);
    private static final LocalTime SENDING_START = LocalTime.of(10, 0);
    // 21:00에 기록한 정산 건이 수정 시간을 다 받고 나가는 시각까지만 예약을 잡는다.
    private static final LocalTime SCHEDULING_END = LocalTime.of(21, 30);
    // 폴링 주기와 재시도로 몇 초에서 몇 분 늦어진 메시지가 다음 날로 밀리지 않게, 실제 발송은 이 시각 전까지 허용한다.
    private static final LocalTime SENDING_END = LocalTime.of(22, 0);

    // 기록 시각에 수정 시간을 더한 시각이 예약할 수 있는 시간대 안이면 그 시각에, 아니면 그다음 10:00에 보낸다.
    // 09:50에 기록해도 10:20에 보내므로, 10:00 직전에 기록한 정산 건도 수정 시간을 다 받는다.
    public static LocalDateTime scheduledAt(LocalDateTime recordedAt) {
        LocalDateTime earliest = recordedAt.plus(EDIT_WINDOW);
        LocalTime time = earliest.toLocalTime();
        if (!time.isBefore(SENDING_START) && !time.isAfter(SCHEDULING_END)) {
            return earliest;
        }

        return nextSendingStart(earliest);
    }

    public static boolean isSendable(LocalDateTime now) {
        LocalTime time = now.toLocalTime();

        return !time.isBefore(SENDING_START) && time.isBefore(SENDING_END);
    }

    // 바로 보내도 되는 시각이면 그대로, 아니면 그다음 10:00을 돌려준다. 취소 알림처럼 수정 시간 없이 보내는 메시지에 쓴다.
    public static LocalDateTime sendableAt(LocalDateTime now) {
        return isSendable(now) ? now : nextSendingStart(now);
    }

    // 10:00 전이면 그날 10:00, 아니면 다음 날 10:00이다.
    public static LocalDateTime nextSendingStart(LocalDateTime time) {
        if (time.toLocalTime().isBefore(SENDING_START)) {
            return time.toLocalDate().atTime(SENDING_START);
        }

        return time.toLocalDate().plusDays(1).atTime(SENDING_START);
    }
}
