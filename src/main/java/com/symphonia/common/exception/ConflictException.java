package com.symphonia.common.exception;

import com.symphonia.common.exception.error.ErrorCode;

public class ConflictException extends BusinessException {
    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }
}
