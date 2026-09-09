package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.BadRequestException;

public class UnsupportedSocialProviderException extends BadRequestException {

    public UnsupportedSocialProviderException() {
        super(AuthErrorCode.UNSUPPORTED_SOCIAL_PROVIDER);
    }
}
