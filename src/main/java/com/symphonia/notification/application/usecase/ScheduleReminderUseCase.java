package com.symphonia.notification.application.usecase;

import com.symphonia.notification.application.dto.command.ScheduleReminderCommand;

public interface ScheduleReminderUseCase {
    // 정산 메시지를 보낸 정산 건의 분담금이 미입금으로 되돌아가면 24시간 뒤 리마인드를 남긴다.
    // 정산 건을 바꾸는 트랜잭션에서, 정산 건을 저장하기 전에 부른다.
    void scheduleReminder(ScheduleReminderCommand command);
}
