package com.symphonia.catalog.domain.exception;

import com.symphonia.catalog.domain.error.CatalogErrorCode;
import com.symphonia.common.exception.InternalServerException;

public class UndeterminedEvaluationException extends InternalServerException {
    public UndeterminedEvaluationException() {
        super(CatalogErrorCode.UNDETERMINED_EVALUATION);
    }
}
