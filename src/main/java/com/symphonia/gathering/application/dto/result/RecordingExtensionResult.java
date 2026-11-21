package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.Gathering;
import java.time.LocalDateTime;

// recordingClosesAt은 연장한 뒤 기록 기간이 끝나는 시각이다.
public record RecordingExtensionResult(LocalDateTime recordingClosesAt) {
    public static RecordingExtensionResult from(Gathering gathering) {
        return new RecordingExtensionResult(gathering.getRecordingClosesAt());
    }
}
