package com.symphonia.common.exception.handler;

import com.symphonia.common.exception.BusinessException;
import com.symphonia.common.exception.error.CommonErrorCode;
import com.symphonia.common.exception.error.ErrorCode;
import com.symphonia.common.exception.response.ErrorResponse;
import com.symphonia.common.response.StandardResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // BusinessException 처리 (도메인 비즈니스 규칙 위반)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<StandardResponse<ErrorResponse>> handleBusinessException(
            BusinessException ex) {
        log.warn(
                "[BusinessException] code={}, message={}",
                ex.getErrorCode().getCode(),
                ex.getMessage());

        return buildResponse(ex.getErrorCode());
    }

    // 500 에러 처리 (예상치 못한 예외)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardResponse<ErrorResponse>> handleException(Exception ex) {
        log.error("[Unhandled Exception] message={}", ex.getMessage(), ex);

        return buildResponse(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<StandardResponse<ErrorResponse>> buildResponse(ErrorCode errorCode) {
        ErrorResponse errorResponse = ErrorResponse.of(errorCode);
        StandardResponse<ErrorResponse> response =
                StandardResponse.fail(errorCode.getStatus(), errorResponse);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }
}
