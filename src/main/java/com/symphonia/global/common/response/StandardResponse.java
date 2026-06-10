package com.symphonia.global.common.response;

import com.symphonia.global.exception.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

public record StandardResponse<T>(
        @Schema(description = "HTTP 상태 코드") int status,
        @Schema(description = "응답 결과") T result
) {
    public static <T> StandardResponse<T> success(HttpStatus status, T result) {
        return new StandardResponse<>(status.value(), result);
    }

    public static StandardResponse<Void> success(HttpStatus status) {
        return new StandardResponse<>(status.value(), null);
    }

    public static StandardResponse<ErrorResponse> fail(HttpStatus status, ErrorResponse error) {
        return new StandardResponse<>(status.value(), error);
    }
}
