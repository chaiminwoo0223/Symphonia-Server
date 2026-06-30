package com.symphonia.member.domain.policy;

import com.symphonia.global.exception.BusinessException;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.error.MemberErrorCode;

public enum MemberPolicy {
    ;

    public static void validateNotWithdrawn(Member member) {
        if (member.isDeleted()) {
            throw BusinessException.from(MemberErrorCode.MEMBER_ALREADY_WITHDRAWN);
        }
    }

    public static void validateNotDuplicated(boolean exists) {
        if (exists) {
            throw BusinessException.from(MemberErrorCode.MEMBER_ALREADY_EXISTS);
        }
    }
}
