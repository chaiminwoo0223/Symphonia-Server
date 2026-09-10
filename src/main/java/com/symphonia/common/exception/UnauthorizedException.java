package com.symphonia.common.exception;

import com.symphonia.common.exception.error.ErrorCode;

public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
