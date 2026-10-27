package com.symphonia.catalog.domain.exception;

import com.symphonia.catalog.domain.error.CatalogErrorCode;
import com.symphonia.common.exception.NotFoundException;

public class DrinkNotFoundException extends NotFoundException {
    public DrinkNotFoundException() {
        super(CatalogErrorCode.DRINK_NOT_FOUND);
    }
}
