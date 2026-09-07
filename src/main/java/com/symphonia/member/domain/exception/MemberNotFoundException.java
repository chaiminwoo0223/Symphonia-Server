package com.symphonia.member.domain.exception;

import com.symphonia.common.exception.NotFoundException;
import com.symphonia.member.domain.error.MemberErrorCode;

public class MemberNotFoundException extends NotFoundException {

    public MemberNotFoundException() {
        super(MemberErrorCode.MEMBER_NOT_FOUND);
    }
}
