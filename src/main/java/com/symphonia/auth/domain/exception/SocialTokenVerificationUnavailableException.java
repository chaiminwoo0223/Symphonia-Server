package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.ServiceUnavailableException;

public class SocialTokenVerificationUnavailableException extends ServiceUnavailableException {
    public SocialTokenVerificationUnavailableException() {
        super(AuthErrorCode.OAUTH_TOKEN_VERIFICATION_UNAVAILABLE);
    }
}
