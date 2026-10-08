package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.BadRequestException;

public class SocialRequiredInfoMissingException extends BadRequestException {
    public SocialRequiredInfoMissingException() {
        super(AuthErrorCode.SOCIAL_REQUIRED_INFO_MISSING);
    }
}
