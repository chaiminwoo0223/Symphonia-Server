package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class InvalidBankAccountException extends BadRequestException {
    public InvalidBankAccountException() {
        super(GatheringErrorCode.INVALID_BANK_ACCOUNT);
    }
}
