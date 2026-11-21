package com.symphonia.member.presentation.dto.response;

import com.symphonia.member.application.dto.result.WithdrawalCheckResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record WithdrawalCheckResponse(
        @Schema(description = "회원이 모임장이고 개인정보 파기 시각이 지나지 않은 모임 수") int ongoingGatheringCount,
        @Schema(description = "진행 중인 모임들의 개인정보를 파기할 시각 중 가장 늦은 시각. 진행 중인 모임이 없으면 null")
                LocalDateTime lastGatheringDeletesAt) {
    public static WithdrawalCheckResponse from(WithdrawalCheckResult result) {
        return new WithdrawalCheckResponse(
                result.ongoingGatheringCount(), result.lastGatheringDeletesAt());
    }
}
