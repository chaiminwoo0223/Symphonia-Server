package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class SettlementAlreadyNotifiedException extends ConflictException {
    public SettlementAlreadyNotifiedException() {
        super(GatheringErrorCode.SETTLEMENT_ALREADY_NOTIFIED);
    }
}
