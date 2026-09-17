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
