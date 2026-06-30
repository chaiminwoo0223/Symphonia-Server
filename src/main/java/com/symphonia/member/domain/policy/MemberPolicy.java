package com.symphonia.member.domain.policy;

import com.symphonia.global.exception.BusinessException;
import com.symphonia.member.domain.error.MemberErrorCode;

public enum MemberPolicy {
    ;

    public static void validateNotDuplicated(boolean exists) {
        if (exists) {
            throw BusinessException.from(MemberErrorCode.MEMBER_ALREADY_EXISTS);
        }
    }
}
