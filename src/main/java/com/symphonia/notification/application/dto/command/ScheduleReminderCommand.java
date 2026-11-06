package com.symphonia.notification.application.dto.command;

// 결제자가 "아직 안 들어왔어요"로 되돌린 분담금의 참여자와 정산 건이다.
public record ScheduleReminderCommand(Long gatheringId, Long participantId, Long settlementId) {
    public static ScheduleReminderCommand of(
            Long gatheringId, Long participantId, Long settlementId) {
        return new ScheduleReminderCommand(gatheringId, participantId, settlementId);
    }
}
