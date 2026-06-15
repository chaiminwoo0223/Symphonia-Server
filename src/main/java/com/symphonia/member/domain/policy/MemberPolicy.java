package com.symphonia.member.domain.policy;

import com.symphonia.global.exception.BusinessException;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.error.MemberErrorCode;

public enum MemberPolicy {
    ;

    public static void validateNotDeleted(Member member) {
        if (member.isDeleted()) {
            throw new BusinessException(MemberErrorCode.MEMBER_ALREADY_DELETED);
        }
    }

    public static void validateDeleted(Member member) {
        if (!member.isDeleted()) {
            throw new BusinessException(MemberErrorCode.MEMBER_NOT_DELETED);
        }
    }

    public static void validateNotDuplicated(boolean exists) {
        if (exists) {
            throw new BusinessException(MemberErrorCode.MEMBER_ALREADY_EXISTS);
        }
    }
}
