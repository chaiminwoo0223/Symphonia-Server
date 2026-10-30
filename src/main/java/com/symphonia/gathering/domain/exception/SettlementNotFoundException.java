package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.NotFoundException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class SettlementNotFoundException extends NotFoundException {
    public SettlementNotFoundException() {
        super(GatheringErrorCode.SETTLEMENT_NOT_FOUND);
    }
}
