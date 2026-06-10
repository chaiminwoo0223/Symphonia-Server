package com.symphonia.global.exception.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ValidationErrorResponse(
        @Schema(description = "오류 필드명") String field,

        @Schema(description = "오류 메시지") String reason
) {
    public static ValidationErrorResponse of(String field, String reason) {
        return new ValidationErrorResponse(field, reason);
    }
}
