package com.symphonia.common.exception;

import com.symphonia.common.exception.error.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public static BusinessException from(ErrorCode errorCode) {
        return new BusinessException(errorCode);
    }
}
