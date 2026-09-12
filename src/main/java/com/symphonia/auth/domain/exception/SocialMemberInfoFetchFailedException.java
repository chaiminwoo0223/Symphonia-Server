package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.InternalServerException;

public class SocialMemberInfoFetchFailedException extends InternalServerException {
    public SocialMemberInfoFetchFailedException() {
        super(AuthErrorCode.OAUTH_MEMBER_INFO_FETCH_FAILED);
    }
}
