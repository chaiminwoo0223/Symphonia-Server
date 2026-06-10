package com.symphonia.global.exception.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.symphonia.global.exception.error.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(description = "에러 코드") String code,
        @Schema(description = "에러 메시지") String message,
        @Schema(description = "Validation 실패 시 필드별 오류 목록") List<ValidationErrorResponse> violations
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, List<ValidationErrorResponse> violations) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), violations);
    }
}
