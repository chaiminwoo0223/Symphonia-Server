package com.symphonia.auth.domain.exception;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.common.exception.BusinessException;

// 공통 5종 예외 중 맞는 상태 코드(500)가 없어 BusinessException을 직접 상속한다.
public class SocialMemberInfoFetchFailedException extends BusinessException {
    public SocialMemberInfoFetchFailedException() {
        super(AuthErrorCode.OAUTH_MEMBER_INFO_FETCH_FAILED);
    }
}
