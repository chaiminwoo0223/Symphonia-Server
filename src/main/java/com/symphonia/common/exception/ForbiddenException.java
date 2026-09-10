package com.symphonia.common.exception;

import com.symphonia.common.exception.error.ErrorCode;

public class ForbiddenException extends BusinessException {
    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
