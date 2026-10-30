package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class BankAccountConsentRequiredException extends BadRequestException {
    public BankAccountConsentRequiredException() {
        super(GatheringErrorCode.BANK_ACCOUNT_CONSENT_REQUIRED);
    }
}
