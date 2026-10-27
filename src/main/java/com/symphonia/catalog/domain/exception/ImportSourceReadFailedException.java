package com.symphonia.catalog.domain.exception;

import com.symphonia.common.exception.InternalServerException;
import com.symphonia.pairing.domain.error.PairingErrorCode;

public class ImportSourceReadFailedException extends InternalServerException {
    public ImportSourceReadFailedException() {
        super(PairingErrorCode.IMPORT_SOURCE_READ_FAILED);
    }
}
