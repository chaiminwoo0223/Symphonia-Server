package com.symphonia.global.exception.handler;

import com.symphonia.global.common.response.StandardResponse;
import com.symphonia.global.exception.error.CommonErrorCode;
import com.symphonia.global.exception.error.ErrorCode;
import com.symphonia.global.exception.response.ErrorResponse;
import com.symphonia.global.exception.response.ValidationErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionHandler extends ResponseEntityExceptionHandler {

    // ResponseEntityExceptionHandler가 처리하는 모든 예외 공통 처리
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode,
            @NonNull WebRequest request
    ) {
        log.warn("[ExceptionInternal] message={}", ex.getMessage());

        return super.handleExceptionInternal(ex, toStandardResponse(CommonErrorCode.INTERNAL_SERVER_ERROR), headers, statusCode, request);
    }

    // 잘못된 URL 요청 시 발생 (404 Not Found)
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex,
            @Nullable HttpHeaders headers,
            @Nullable HttpStatusCode status,
            @Nullable WebRequest request
    ) {
        log.warn("[NoResourceFoundException] url={}", ex.getResourcePath());

        return buildErrorResponse(CommonErrorCode.NOT_FOUND);
    }

    // JSON 형식 오류 또는 필드 타입 불일치 시 발생
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            @Nullable HttpHeaders headers,
            @Nullable HttpStatusCode statusCode,
            @Nullable WebRequest request
    ) {
        log.warn("[HttpMessageNotReadableException] message={}", ex.getMessage());

        return buildErrorResponse(CommonErrorCode.INVALID_JSON_FORMAT);
    }

    // 지원하지 않는 HTTP 메서드 요청 시 발생 (405 Method Not Allowed)
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            @Nullable HttpHeaders headers,
            @Nullable HttpStatusCode statusCode,
            @Nullable WebRequest request
    ) {
        log.warn("[HttpRequestMethodNotSupportedException] method={}", ex.getMethod());

        return buildErrorResponse(CommonErrorCode.METHOD_NOT_ALLOWED);
    }

    // 필수 RequestParam 누락 시 발생
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            @Nullable HttpHeaders headers,
            @Nullable HttpStatusCode status,
            @Nullable WebRequest request
    ) {
        log.warn("[MissingServletRequestParameterException] paramName={}", ex.getParameterName());

        return buildErrorResponse(CommonErrorCode.MISSING_REQUEST_PARAMETER);
    }

    // @Valid, @Validated 바인딩 오류 시 발생 (주로 @RequestBody)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @Nullable HttpHeaders headers,
            @Nullable HttpStatusCode statusCode,
            @Nullable WebRequest request
    ) {
        log.warn("[MethodArgumentNotValidException] message={}", ex.getMessage());

        List<ValidationErrorResponse> violations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toValidationErrorResponse)
                .toList();

        return buildValidationResponse(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID, violations);
    }

    // PathVariable, RequestParam, RequestHeader에서 파라미터 타입 불일치 시 발생
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("[MethodArgumentTypeMismatchException] message={}", ex.getMessage());

        return buildErrorResponse(CommonErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH);
    }

    // RequestParam, PathVariable 등 제약 조건 위반 시 발생
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("[ConstraintViolationException] message={}", ex.getMessage());

        List<ValidationErrorResponse> violations = ex.getConstraintViolations()
                .stream()
                .map(this::toValidationErrorResponse)
                .toList();

        return buildValidationResponse(CommonErrorCode.CONSTRAINT_VIOLATION, violations);
    }

    private ResponseEntity<Object> buildErrorResponse(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(toStandardResponse(errorCode));
    }

    private ResponseEntity<Object> buildValidationResponse(ErrorCode errorCode, List<ValidationErrorResponse> violations) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(toStandardResponse(errorCode, violations));
    }

    private StandardResponse<ErrorResponse> toStandardResponse(ErrorCode errorCode) {
        return StandardResponse.fail(errorCode.getStatus(), ErrorResponse.of(errorCode));
    }

    private StandardResponse<ErrorResponse> toStandardResponse(ErrorCode errorCode, List<ValidationErrorResponse> violations) {
        return StandardResponse.fail(errorCode.getStatus(), ErrorResponse.of(errorCode, violations));
    }

    private ValidationErrorResponse toValidationErrorResponse(FieldError fieldError) {
        return ValidationErrorResponse.of(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ValidationErrorResponse toValidationErrorResponse(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        String field = path.substring(path.lastIndexOf(".") + 1);
        return ValidationErrorResponse.of(field, violation.getMessage());
    }
}
