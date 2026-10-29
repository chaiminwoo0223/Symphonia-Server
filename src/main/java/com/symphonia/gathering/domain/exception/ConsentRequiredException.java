package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class ConsentRequiredException extends BadRequestException {
    public ConsentRequiredException() {
        super(GatheringErrorCode.CONSENT_REQUIRED);
    }
}
