package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.GatheringResult;
import com.symphonia.gathering.domain.vo.JoinClosedReason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record GatheringResponse(
        @Schema(description = "모임 이름") String name,
        @Schema(description = "참여 가능 여부. 참여 기간이 끝났거나 참여 인원이 가득 차면 false입니다.") boolean joinable,
        @Schema(
                        description =
                                "참여할 수 없는 사유. 참여할 수 있으면 null입니다. 기간과 인원이 모두 해당하면 PERIOD_EXPIRED입니다.",
                        nullable = true)
                JoinClosedReason joinClosedReason,
        @Schema(description = "참여할 때 함께 보낼 동의 안내 버전") String consentVersion,
        @Schema(description = "모임의 개인정보를 파기할 예정 시각 (모든 분담금이 끝나면 앞당겨짐)") LocalDateTime deletesAt) {
    public static GatheringResponse from(GatheringResult result) {
        return new GatheringResponse(
                result.name(),
                result.joinable(),
                result.joinClosedReason(),
                result.consentVersion(),
                result.deletesAt());
    }
}
