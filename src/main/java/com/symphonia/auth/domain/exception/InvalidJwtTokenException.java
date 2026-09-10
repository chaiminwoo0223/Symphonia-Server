package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.UnauthorizedException;

public class InvalidJwtTokenException extends UnauthorizedException {
    public InvalidJwtTokenException() {
        super(AuthErrorCode.INVALID_JWT_TOKEN);
    }
}
