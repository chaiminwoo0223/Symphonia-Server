package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class SettlementAlreadyCancelledException extends ConflictException {
    public SettlementAlreadyCancelledException() {
        super(GatheringErrorCode.SETTLEMENT_ALREADY_CANCELLED);
    }
}
