package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.GatheringJoinInfoResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record GatheringJoinInfoResponse(
        @Schema(description = "모임 이름") String name,
        @Schema(description = "참여 가능 여부") boolean joinable,
        @Schema(description = "참여할 때 함께 보낼 동의 안내 버전") String consentVersion) {
    public static GatheringJoinInfoResponse from(GatheringJoinInfoResult result) {
        return new GatheringJoinInfoResponse(
                result.name(), result.joinable(), result.consentVersion());
    }
}
