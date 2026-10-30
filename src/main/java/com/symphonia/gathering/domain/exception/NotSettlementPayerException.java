package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ForbiddenException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class NotSettlementPayerException extends ForbiddenException {
    public NotSettlementPayerException() {
        super(GatheringErrorCode.NOT_SETTLEMENT_PAYER);
    }
}
