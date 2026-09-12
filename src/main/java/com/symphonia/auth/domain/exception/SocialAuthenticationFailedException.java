package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.InternalServerException;

public class SocialAuthenticationFailedException extends InternalServerException {
    public SocialAuthenticationFailedException() {
        super(AuthErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED);
    }
}
