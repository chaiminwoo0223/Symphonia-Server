package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.GatheringResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record GatheringResponse(
        @Schema(description = "모임 이름") String name,
        @Schema(description = "참여 가능 여부") boolean joinable,
        @Schema(description = "참여할 때 함께 보낼 동의 안내 버전") String consentVersion,
        @Schema(description = "모임의 개인정보를 파기할 예정 시각 (모든 분담금이 끝나면 앞당겨짐)") LocalDateTime deletesAt) {
    public static GatheringResponse from(GatheringResult result) {
        return new GatheringResponse(
                result.name(), result.joinable(), result.consentVersion(), result.deletesAt());
    }
}
