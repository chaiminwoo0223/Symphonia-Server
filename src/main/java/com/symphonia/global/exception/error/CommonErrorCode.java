package com.symphonia.global.exception.error;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // 400
    METHOD_ARGUMENT_NOT_VALID(HttpStatus.BAD_REQUEST, "요청 본문(JSON)의 값 유효성 검증에 실패했습니다."),
    METHOD_ARGUMENT_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "요청한 메서드 파라미터의 타입이 일치하지 않습니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다."),
    CONSTRAINT_VIOLATION(HttpStatus.BAD_REQUEST, "요청 파라미터 또는 경로 변수 유효성 검증에 실패했습니다."),
    INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, "요청 본문(JSON) 형식이 잘못되어 파싱할 수 없습니다. (필드 타입 불일치, 필수 필드 누락 등)"),
    INVALID_ORIGIN(HttpStatus.BAD_REQUEST, "잘못된 Origin입니다."),
    UNSUPPORTED_ORIGIN(HttpStatus.BAD_REQUEST, "지원하지 않는 Origin입니다."),

    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),

    // 405
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드 입니다."),

    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류입니다. 관리자에게 문의하세요."),
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
