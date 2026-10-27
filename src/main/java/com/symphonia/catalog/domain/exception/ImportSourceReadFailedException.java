package com.symphonia.catalog.domain.exception;

import com.symphonia.catalog.domain.error.CatalogErrorCode;
import com.symphonia.common.exception.InternalServerException;

public class ImportSourceReadFailedException extends InternalServerException {
    public ImportSourceReadFailedException() {
        super(CatalogErrorCode.IMPORT_SOURCE_READ_FAILED);
    }
}
