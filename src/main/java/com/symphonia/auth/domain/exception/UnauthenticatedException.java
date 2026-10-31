package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.UnauthorizedException;

public class UnauthenticatedException extends UnauthorizedException {
    public UnauthenticatedException() {
        super(AuthErrorCode.UNAUTHENTICATED);
    }
}
