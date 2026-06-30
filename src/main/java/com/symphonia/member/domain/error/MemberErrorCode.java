package com.symphonia.member.domain.error;

import com.symphonia.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {
    // 400
    MEMBER_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "이미 탈퇴한 멤버입니다."),

    // 404
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "요창한 멤버를 찾을 수 없습니다."),

    // 409
    MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 회원가입된 멤버입니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }
}
