package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.UnauthorizedException;

public class RefreshTokenNotFoundException extends UnauthorizedException {

    public RefreshTokenNotFoundException() {
        super(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }
}
