package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class SettlementConflictException extends ConflictException {
    public SettlementConflictException() {
        super(GatheringErrorCode.SETTLEMENT_CONFLICT);
    }
}
