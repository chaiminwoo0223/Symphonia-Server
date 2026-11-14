package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.BadRequestException;

public class InvalidSocialTokenException extends BadRequestException {
    public InvalidSocialTokenException() {
        super(AuthErrorCode.INVALID_SOCIAL_TOKEN);
    }
}
