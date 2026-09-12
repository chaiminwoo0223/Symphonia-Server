package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.InternalServerException;

public class AccessTokenHashingFailedException extends InternalServerException {
    public AccessTokenHashingFailedException() {
        super(AuthErrorCode.ACCESS_TOKEN_HASHING_FAILED);
    }
}
