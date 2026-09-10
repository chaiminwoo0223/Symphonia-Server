package com.symphonia.common.exception;

import com.symphonia.common.exception.error.ErrorCode;

public class BadRequestException extends BusinessException {
    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }
}
