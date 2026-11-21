package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.ExtendRecordingCommand;
import com.symphonia.gathering.application.dto.result.RecordingExtensionResult;

public interface ExtendRecordingUseCase {
    RecordingExtensionResult extend(ExtendRecordingCommand command);
}
