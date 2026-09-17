package com.symphonia.pairing.domain.exception;

import com.symphonia.common.exception.NotFoundException;
import com.symphonia.pairing.domain.error.PairingErrorCode;

public class DrinkNotFoundException extends NotFoundException {
    public DrinkNotFoundException() {
        super(PairingErrorCode.DRINK_NOT_FOUND);
    }
}
