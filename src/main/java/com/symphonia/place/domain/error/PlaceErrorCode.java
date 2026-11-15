package com.symphonia.place.domain.error;

import com.symphonia.common.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum PlaceErrorCode implements ErrorCode {
    // 503
    PLACE_SEARCH_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE, "맛집 검색을 잠시 이용할 수 없습니다. 잠시 후 다시 시도해주세요."),
    PLACE_SEARCH_LIMIT_EXCEEDED(
            HttpStatus.SERVICE_UNAVAILABLE, "맛집 검색 요청이 많아 이용할 수 없습니다. 잠시 후 다시 시도해주세요."),
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
