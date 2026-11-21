package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.HostedGatheringResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record HostedGatheringResponse(
        @Schema(description = "모임 ID") Long gatheringId,
        @Schema(description = "모임 이름") String name,
        @Schema(description = "모임을 연 시각") LocalDateTime openedAt,
        @Schema(description = "기록 기간이 끝나는 시각. 이 시각부터는 정산 건 기록, 차수 추가, 참여를 할 수 없습니다.")
                LocalDateTime recordingClosesAt,
        @Schema(description = "기록 기간을 연장했는지 여부") boolean recordingExtended,
        @Schema(description = "개인정보 파기 예정 시각. 이 시각이 지나면 모임을 찾을 수 없습니다.") LocalDateTime deletesAt,
        @Schema(description = "참여자 수") long participantCount) {
    public static HostedGatheringResponse from(HostedGatheringResult result) {
        return new HostedGatheringResponse(
                result.gatheringId(),
                result.name(),
                result.openedAt(),
                result.recordingClosesAt(),
                result.recordingExtended(),
                result.deletesAt(),
                result.participantCount());
    }
}
