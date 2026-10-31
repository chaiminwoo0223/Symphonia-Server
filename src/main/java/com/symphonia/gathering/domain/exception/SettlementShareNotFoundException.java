package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.NotFoundException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class SettlementShareNotFoundException extends NotFoundException {
    public SettlementShareNotFoundException() {
        super(GatheringErrorCode.SETTLEMENT_SHARE_NOT_FOUND);
    }
}
