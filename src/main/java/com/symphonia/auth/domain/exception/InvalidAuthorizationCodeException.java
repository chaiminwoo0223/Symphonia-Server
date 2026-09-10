package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.BadRequestException;

public class InvalidAuthorizationCodeException extends BadRequestException {
    public InvalidAuthorizationCodeException() {
        super(AuthErrorCode.INVALID_AUTHORIZATION_CODE);
    }
}
