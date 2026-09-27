package com.symphonia.pairing.domain.exception;

import com.symphonia.common.exception.InternalServerException;
import com.symphonia.pairing.domain.error.PairingErrorCode;

public class DrinkImportSourceReadFailedException extends InternalServerException {
    public DrinkImportSourceReadFailedException() {
        super(PairingErrorCode.DRINK_IMPORT_SOURCE_READ_FAILED);
    }
}
