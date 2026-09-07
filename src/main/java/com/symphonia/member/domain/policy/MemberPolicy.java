package com.symphonia.member.domain.policy;

import com.symphonia.member.domain.exception.MemberAlreadyExistsException;

public enum MemberPolicy {
    ;

    public static void validateNotDuplicated(boolean exists) {
        if (exists) {
            throw new MemberAlreadyExistsException();
        }
    }
}
