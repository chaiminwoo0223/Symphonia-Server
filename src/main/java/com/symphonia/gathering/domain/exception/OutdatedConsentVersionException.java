package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class OutdatedConsentVersionException extends BadRequestException {
    public OutdatedConsentVersionException() {
        super(GatheringErrorCode.OUTDATED_CONSENT_VERSION);
    }
}
