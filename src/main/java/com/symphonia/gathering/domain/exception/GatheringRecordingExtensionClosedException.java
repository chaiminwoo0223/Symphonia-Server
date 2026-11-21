package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class GatheringRecordingExtensionClosedException extends BadRequestException {
    public GatheringRecordingExtensionClosedException() {
        super(GatheringErrorCode.GATHERING_RECORDING_EXTENSION_CLOSED);
    }
}
