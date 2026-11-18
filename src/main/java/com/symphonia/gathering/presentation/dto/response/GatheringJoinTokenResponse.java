package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.GatheringJoinTokenResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record GatheringJoinTokenResponse(
        @Schema(description = "모임 ID") Long gatheringId,
        @Schema(description = "참여 QR과 링크에 담을 참여 토큰") String joinToken,
        @Schema(description = "참여 토큰으로 참여할 수 있는 기간이 끝나는 시각. 이 시각부터는 참여할 수 없습니다.")
                LocalDateTime expiresAt) {
    public static GatheringJoinTokenResponse from(GatheringJoinTokenResult result) {
        return new GatheringJoinTokenResponse(
                result.gatheringId(), result.joinToken(), result.expiresAt());
    }
}
