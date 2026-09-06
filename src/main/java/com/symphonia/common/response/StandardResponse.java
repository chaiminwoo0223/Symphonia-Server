package com.symphonia.common.response;

import com.symphonia.common.exception.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

public record StandardResponse<T>(
        @Schema(description = "성공 여부") boolean ok,
        @Schema(description = "HTTP 상태 코드") int status,
        @Schema(description = "응답 데이터") T data) {
    public static <T> StandardResponse<T> success(HttpStatus status, T data) {
        return new StandardResponse<>(true, status.value(), data);
    }

    public static StandardResponse<Void> success(HttpStatus status) {
        return new StandardResponse<>(true, status.value(), null);
    }

    public static StandardResponse<ErrorResponse> fail(
            HttpStatus status, ErrorResponse errorResponse) {
        return new StandardResponse<>(false, status.value(), errorResponse);
    }
}
