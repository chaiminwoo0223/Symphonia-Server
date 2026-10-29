package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class InvalidPhoneNumberException extends BadRequestException {
    public InvalidPhoneNumberException() {
        super(GatheringErrorCode.INVALID_PHONE_NUMBER);
    }
}
