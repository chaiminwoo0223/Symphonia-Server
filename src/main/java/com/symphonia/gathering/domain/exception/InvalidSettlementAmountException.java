package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class InvalidSettlementAmountException extends BadRequestException {
    public InvalidSettlementAmountException() {
        super(GatheringErrorCode.INVALID_SETTLEMENT_AMOUNT);
    }
}
