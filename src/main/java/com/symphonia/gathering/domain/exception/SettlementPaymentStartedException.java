package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class SettlementPaymentStartedException extends ConflictException {
    public SettlementPaymentStartedException() {
        super(GatheringErrorCode.SETTLEMENT_PAYMENT_STARTED);
    }
}
