package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class SettlementSharesSentException extends ConflictException {
    public SettlementSharesSentException() {
        super(GatheringErrorCode.SETTLEMENT_SHARES_SENT);
    }
}
