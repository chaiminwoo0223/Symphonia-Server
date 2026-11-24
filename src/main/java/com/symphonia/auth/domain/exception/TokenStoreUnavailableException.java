package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.ServiceUnavailableException;

public class TokenStoreUnavailableException extends ServiceUnavailableException {
    public TokenStoreUnavailableException(Throwable cause) {
        super(AuthErrorCode.TOKEN_STORE_UNAVAILABLE);
        initCause(cause);
    }
}
