package com.symphonia.member.application.dto.result;

import com.symphonia.gathering.application.dto.result.OngoingGatheringsResult;
import java.time.LocalDateTime;

// 탈퇴하기 전에 알려 줄 진행 중인 모임이다. 회원이 모임장이고 파기 시각이 지나지 않은 모임만 센다.
// lastGatheringDeletesAt은 그 모임들의 개인정보를 파기할 시각 중 가장 늦은 시각이고, 진행 중인 모임이 없으면 null이다.
public record WithdrawalCheckResult(
        int ongoingGatheringCount, LocalDateTime lastGatheringDeletesAt) {
    public static WithdrawalCheckResult from(OngoingGatheringsResult gatherings) {
        return new WithdrawalCheckResult(gatherings.count(), gatherings.lastDeletesAt());
    }
}
