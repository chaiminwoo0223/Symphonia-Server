package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class OutdatedBankAccountConsentVersionException extends BadRequestException {
    public OutdatedBankAccountConsentVersionException() {
        super(GatheringErrorCode.OUTDATED_BANK_ACCOUNT_CONSENT_VERSION);
    }
}
