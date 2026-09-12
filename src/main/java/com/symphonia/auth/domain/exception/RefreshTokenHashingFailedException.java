package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.InternalServerException;

public class RefreshTokenHashingFailedException extends InternalServerException {
    public RefreshTokenHashingFailedException() {
        super(AuthErrorCode.REFRESH_TOKEN_HASHING_FAILED);
    }
}
