package com.symphonia.pairing.domain.error;

import com.symphonia.common.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum PairingErrorCode implements ErrorCode {
    // 404
    DRINK_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 술을 찾을 수 없습니다."),
    ANJU_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 안주를 찾을 수 없습니다."),
    MUSIC_MOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 음악 무드를 찾을 수 없습니다."),

    // 500
    IMPORT_SOURCE_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "적재 원본 파일을 읽을 수 없습니다."),
    INVALID_TOP_LIMIT(HttpStatus.INTERNAL_SERVER_ERROR, "상위 후보 개수는 1 이상이어야 합니다."),
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
