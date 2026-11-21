package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.RecordingExtensionResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record RecordingExtensionResponse(
        @Schema(description = "연장한 뒤 기록 기간이 끝나는 시각. 이 시각부터는 정산 건 기록, 차수 추가, 참여, 페어링 변경을 할 수 없습니다.")
                LocalDateTime recordingClosesAt) {
    public static RecordingExtensionResponse from(RecordingExtensionResult result) {
        return new RecordingExtensionResponse(result.recordingClosesAt());
    }
}
