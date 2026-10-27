package com.symphonia.catalog.domain.exception;

import com.symphonia.catalog.domain.error.CatalogErrorCode;
import com.symphonia.common.exception.NotFoundException;

public class AnjuNotFoundException extends NotFoundException {
    public AnjuNotFoundException() {
        super(CatalogErrorCode.ANJU_NOT_FOUND);
    }
}
