package com.symphonia.auth.domain.error;

import com.symphonia.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    // 400
    MISSING_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "리프레시 토큰이 누락되었습니다."),

    // 401
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "인증되지 않은 요청입니다."),

    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 존재하지 않습니다."),
    REFRESH_TOKEN_STOLEN(HttpStatus.UNAUTHORIZED, "탈취된 리프레시 토큰입니다."),

    TOKEN_IS_BLACKLISTED(HttpStatus.UNAUTHORIZED, "블랙리스트된 엑세스 토큰입니다."),

    INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "잘못된 JWT 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "잘못된 리프레시 토큰입니다."),

    // 403
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 부족합니다."),
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
