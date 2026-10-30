package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class BankAccountAlreadyRegisteredException extends ConflictException {
    public BankAccountAlreadyRegisteredException() {
        super(GatheringErrorCode.BANK_ACCOUNT_ALREADY_REGISTERED);
    }
}
