package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.InternalServerException;

public class SocialTokenVerificationFailedException extends InternalServerException {
    public SocialTokenVerificationFailedException() {
        super(AuthErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
    }
}
