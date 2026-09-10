package com.symphonia.member.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.member.domain.error.MemberErrorCode;

public class MemberAlreadyExistsException extends ConflictException {
    public MemberAlreadyExistsException() {
        super(MemberErrorCode.MEMBER_ALREADY_EXISTS);
    }
}
