package com.symphonia.pairing.domain.error;

import com.symphonia.common.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum PairingErrorCode implements ErrorCode {
    // 500
    INVALID_TOP_LIMIT(HttpStatus.INTERNAL_SERVER_ERROR, "상위로 고를 개수는 1 이상이어야 합니다."),
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
