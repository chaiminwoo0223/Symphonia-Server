package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class FixedAmountExceedsTotalException extends BadRequestException {
    public FixedAmountExceedsTotalException() {
        super(GatheringErrorCode.FIXED_AMOUNT_EXCEEDS_TOTAL);
    }
}
