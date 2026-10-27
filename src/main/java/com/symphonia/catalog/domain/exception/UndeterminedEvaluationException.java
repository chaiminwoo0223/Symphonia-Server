package com.symphonia.catalog.domain.exception;

import com.symphonia.common.exception.InternalServerException;
import com.symphonia.pairing.domain.error.PairingErrorCode;

public class UndeterminedEvaluationException extends InternalServerException {
    public UndeterminedEvaluationException() {
        super(PairingErrorCode.UNDETERMINED_EVALUATION);
    }
}
