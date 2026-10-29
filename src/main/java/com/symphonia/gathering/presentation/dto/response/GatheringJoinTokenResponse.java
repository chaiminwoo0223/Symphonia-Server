package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.GatheringJoinTokenResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record GatheringJoinTokenResponse(
        @Schema(description = "모임 ID") Long gatheringId,
        @Schema(description = "참여 QR과 링크에 담을 참여 토큰") String joinToken) {
    public static GatheringJoinTokenResponse from(GatheringJoinTokenResult result) {
        return new GatheringJoinTokenResponse(result.gatheringId(), result.joinToken());
    }
}
