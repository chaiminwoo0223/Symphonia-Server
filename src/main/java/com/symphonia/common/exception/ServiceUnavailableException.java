package com.symphonia.common.exception;

import com.symphonia.common.exception.error.ErrorCode;

public class ServiceUnavailableException extends BusinessException {
    public ServiceUnavailableException(ErrorCode errorCode) {
        super(errorCode);
    }
}
