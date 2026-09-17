package com.symphonia.pairing.domain.exception;

import com.symphonia.common.exception.NotFoundException;
import com.symphonia.pairing.domain.error.PairingErrorCode;

public class AnjuNotFoundException extends NotFoundException {
    public AnjuNotFoundException() {
        super(PairingErrorCode.ANJU_NOT_FOUND);
    }
}
