package com.symphonia.auth.domain.error;

import com.symphonia.common.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    // 400
    UNSUPPORTED_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 제공자입니다."),
    INVALID_SOCIAL_TOKEN(HttpStatus.BAD_REQUEST, "만료되었거나 유효하지 않은 소셜 로그인 토큰입니다."),
    SOCIAL_REQUIRED_INFO_MISSING(HttpStatus.BAD_REQUEST, "소셜 계정의 이메일과 닉네임 제공에 동의해야 이용할 수 있습니다."),

    // 401
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "인증되지 않은 요청입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 존재하지 않습니다. 다시 로그인해주세요."),

    // 403
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 부족합니다."),

    // 503
    OAUTH_TOKEN_VERIFICATION_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE, "소셜 로그인 확인을 잠시 이용할 수 없습니다. 잠시 후 다시 시도해주세요."),
    OAUTH_MEMBER_INFO_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE, "소셜 계정 정보 조회를 잠시 이용할 수 없습니다. 잠시 후 다시 시도해주세요."),
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
