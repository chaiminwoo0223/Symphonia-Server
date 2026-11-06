package com.symphonia.notification.domain.policy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// 늦은 밤이나 새벽에 돈 이야기가 담긴 메시지를 받지 않게, 정산 메시지와 알림을 보낼 수 있는 시간대를 정한다.
// 미입금 리마인드를 보내는 시점과 간격도 정한다(#127).
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
    // 정산 메시지를 보낸 뒤 자동 리마인드를 보내는 시점이다. 정산 메시지 한 번에 리마인드는 최대 두 번이다.
    // NotificationDelivery.forReminders가 새 리마인드끼리 겹치는지 보지 않으므로, 시점끼리는 REMINDER_GAP 이상 떨어뜨린다.
    private static final List<Duration> REMINDER_DELAYS =
            List.of(Duration.ofDays(3), Duration.ofDays(5));
    // 결제자가 "아직 안 들어왔어요"로 되돌린 뒤 리마인드를 보내기까지 기다리는 시간이다.
    private static final Duration REVERT_REMINDER_DELAY = Duration.ofHours(24);
    // 같은 참여자에게 이 간격 안에 리마인드를 두 번 보내지 않는다.
    private static final Duration REMINDER_GAP = Duration.ofHours(24);

    // 기록 시각에 수정 시간을 더한 시각이 예약할 수 있는 시간대 안이면 그 시각에, 아니면 그다음 10:00에 보낸다.
    // 09:50에 기록해도 10:20에 보내므로, 10:00 직전에 기록한 정산 건도 수정 시간을 다 받는다.
    public static LocalDateTime scheduledAt(LocalDateTime recordedAt) {
        LocalDateTime earliest = recordedAt.plus(EDIT_WINDOW);

        return isSchedulable(earliest) ? earliest : nextSendingStart(earliest);
    }

    public static boolean isSendable(LocalDateTime now) {
        LocalTime time = now.toLocalTime();

        return !time.isBefore(SENDING_START) && time.isBefore(SENDING_END);
    }

    // 바로 보내도 되는 시각이면 그대로, 아니면 그다음 10:00을 돌려준다. 취소 알림처럼 수정 시간 없이 보내는 메시지에 쓴다.
    public static LocalDateTime sendableAt(LocalDateTime now) {
        return isSendable(now) ? now : nextSendingStart(now);
    }

    // 3일과 5일은 예정 시각이 아니라 정산 메시지를 실제로 보내기 시작한 시각부터 센다.
    public static List<LocalDateTime> reminderTimes(LocalDateTime notifiedAt) {
        return REMINDER_DELAYS.stream().map(delay -> sendableAt(notifiedAt.plus(delay))).toList();
    }

    public static LocalDateTime revertReminderTime(LocalDateTime revertedAt) {
        return sendableAt(revertedAt.plus(REVERT_REMINDER_DELAY));
    }

    public static boolean isWithinReminderGap(LocalDateTime time, LocalDateTime otherTime) {
        return Duration.between(time, otherTime).abs().compareTo(REMINDER_GAP) < 0;
    }

    // 10:00 전이면 그날 10:00, 아니면 다음 날 10:00이다.
    public static LocalDateTime nextSendingStart(LocalDateTime time) {
        LocalDateTime todaySendingStart = time.toLocalDate().atTime(SENDING_START);

        return time.isBefore(todaySendingStart) ? todaySendingStart : todaySendingStart.plusDays(1);
    }

    private static boolean isSchedulable(LocalDateTime time) {
        LocalTime localTime = time.toLocalTime();

        return !localTime.isBefore(SENDING_START) && !localTime.isAfter(SCHEDULING_END);
    }
}
