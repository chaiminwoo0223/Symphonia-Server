package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.ServiceUnavailableException;

public class SocialMemberInfoUnavailableException extends ServiceUnavailableException {
    public SocialMemberInfoUnavailableException() {
        super(AuthErrorCode.OAUTH_MEMBER_INFO_UNAVAILABLE);
    }
}
