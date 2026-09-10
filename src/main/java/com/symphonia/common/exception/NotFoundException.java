package com.symphonia.common.exception;

import com.symphonia.common.exception.error.ErrorCode;

public class NotFoundException extends BusinessException {
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
