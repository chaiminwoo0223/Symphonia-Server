package com.symphonia.pairing.domain.exception;

import com.symphonia.common.exception.InternalServerException;
import com.symphonia.pairing.domain.error.PairingErrorCode;

public class InvalidTopLimitException extends InternalServerException {
    public InvalidTopLimitException() {
        super(PairingErrorCode.INVALID_TOP_LIMIT);
    }
}
