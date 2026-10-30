package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class SettlementRecordingClosedException extends BadRequestException {
    public SettlementRecordingClosedException() {
        super(GatheringErrorCode.SETTLEMENT_RECORDING_CLOSED);
    }
}
