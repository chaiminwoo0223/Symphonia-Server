package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class BankAccountRequiredException extends BadRequestException {
    public BankAccountRequiredException() {
        super(GatheringErrorCode.BANK_ACCOUNT_REQUIRED);
    }
}
