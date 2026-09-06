---
name: error-handling
description: Load when handling exceptions/errors. BusinessException 계층(공통 HTTP 상태별 예외 + 도메인별 구체 예외), ErrorCode, GlobalExceptionHandler/ValidationExceptionHandler 이중 구조, StandardResponse 에러 포맷, Bean Validation 변환.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Error-Handling Rules

## 구성 요소

| 요소 | 위치 | 역할 |
|---|---|---|
| `ErrorCode` (인터페이스) | `common.exception.error` | `getStatus()`(HttpStatus)·`getCode()`·`getMessage()` 계약. 도메인/공통 에러 코드 enum이 구현 |
| `BusinessException` | `common.exception` | 모든 비즈니스 예외의 베이스. `errorCode: ErrorCode`를 갖는다. **서브클래싱을 전제로 한다** (아래 "예외 계층" 참고) |
| 공통 예외 (`BadRequestException` 등) | `common.exception` | HTTP 상태별 얇은 래퍼: `BadRequestException`/`UnauthorizedException`/`ForbiddenException`/`NotFoundException`/`ConflictException` |
| 도메인 구체 예외 (`MemberNotFoundException` 등) | 각 도메인의 `domain/exception` 패키지 | 공통 예외를 상속해 에러 코드를 고정한 도메인 전용 예외 |
| 도메인 `*ErrorCode` (enum) | 각 도메인의 `domain/error` 패키지 | `ErrorCode` 구현. 도메인별 에러 정의 (`MemberErrorCode`, `AuthErrorCode`) |
| `CommonErrorCode` (enum) | `common.exception.error` | 도메인 무관 공통 에러 코드. 주로 프레임워크 예외를 매핑하는 데 쓴다 |
| `GlobalExceptionHandler` | `common.exception.handler` | `@RestControllerAdvice`. `BusinessException`(서브클래스 포함)과 그 외 미처리 `Exception`(catch-all)을 `StandardResponse`로 변환 |
| `ValidationExceptionHandler` | `common.exception.handler` | `ResponseEntityExceptionHandler` 상속, `@Order(HIGHEST_PRECEDENCE)`. Bean Validation·파라미터·URL 등 스프링 프레임워크 예외를 `CommonErrorCode`로 매핑 |
| `ErrorResponse` / `ValidationErrorResponse` | `common.exception.response` | 에러 응답 바디. 필드별 검증 오류는 `violations: List<ValidationErrorResponse>`(`field`+`reason`)로 표현 |

Symphonia는 단일 모듈이라 todakun처럼 `common-web` 모듈을 따로 두지 않는다. 예외 관련 컴포넌트는 모두 `common.exception` 패키지 아래 둔다.

**핸들러가 2개인 이유**: `GlobalExceptionHandler`는 도메인 비즈니스 예외(`BusinessException`)와 예상 못한 500(catch-all)만 담당하고, `ValidationExceptionHandler`는 스프링이 던지는 프레임워크 예외(Bean Validation, 파라미터, URL 등)만 담당한다. 관심사가 다르므로 핸들러를 하나로 합치지 않는다.

> ⚠️ **목표 설계, 아직 코드에는 없음 (2026-09-06 결정)**: 아래 "예외 계층"은 목표 설계다. 지금 실제 코드는 `BusinessException`을 서브클래싱하지 않는 단일 클래스로 두고 어디서나 `BusinessException.from(errorCode)`로 직접 던진다. 공통 5종 예외 클래스와 도메인별 구체 예외를 실제로 추가하고 기존 `BusinessException.from(...)` 호출부를 교체하는 건 별도 이슈의 구현 작업이다. 새 도메인을 스캐폴딩하거나 예외 관련 코드를 새로 작성할 때는 이 문서 기준(계층 구조)으로 작성하고, 아직 마이그레이션되지 않은 기존 `member`/`auth` 코드를 참고할 땐 거기 남아있는 `BusinessException.from(...)` 호출을 정답으로 여기지 않는다.

## 예외 계층

```
BusinessException (common.exception, errorCode: ErrorCode)
├── BadRequestException / UnauthorizedException / ForbiddenException / NotFoundException / ConflictException (common.exception)
│     └── MemberNotFoundException 등: 도메인별 구체 예외 ({domain}/domain/exception)
└── (공통 5종 중 맞는 게 없는 예외적인 경우에만 도메인이 BusinessException을 직접 상속한다. 기본은 공통 5종 중 하나를 상속하는 것이다)
```

공통 예외는 상태 하나만 대표하는 얇은 래퍼다. 필드나 로직을 추가하지 않는다. `BusinessException`의 생성자는 `protected`로 열어 공통 예외만 상속할 수 있게 하고, 도메인은 공통 예외를 상속해 에러 코드를 생성자에서 고정한다 (호출부가 매번 `ErrorCode`를 넘기지 않는다).

```java
// common/exception
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

// common/exception
public class NotFoundException extends BusinessException {
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}

// member/domain/exception
public class MemberNotFoundException extends NotFoundException {
    public MemberNotFoundException() {
        super(MemberErrorCode.MEMBER_NOT_FOUND);
    }
}
```

## 코드 정의 (도메인별 `*ErrorCode`)

도메인 코드는 `ErrorCode`를 구현하는 enum으로 정의한다 (코드 자체가 HTTP status를 갖는다). `getCode()`는 별도 문자열을 만들지 않고 enum 상수명(`name()`)을 그대로 반환한다.

```java
// member/domain/error
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 멤버를 찾을 수 없습니다."),
    MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 회원가입된 멤버입니다."),
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
```

## GlobalExceptionHandler

`GlobalExceptionHandler`(`@RestControllerAdvice`)는 애플리케이션 전체에 **하나만** 둔다.

- **`BusinessException`만 캐치**하고 `errorCode.getStatus()`로 HTTP status를 결정한다. `@ExceptionHandler(BusinessException.class)`는 스프링이 서브클래스까지 함께 잡아주므로, 공통/도메인 예외가 늘어나도 핸들러에 분기를 추가하지 않는다. 계층을 두는 목적은 타입으로 의도를 드러내는 것이지 핸들러 분기를 늘리는 게 아니다.
- 프레임워크 예외는 `CommonErrorCode`로 매핑하되, **클라이언트 잘못을 500으로 흘리지 않는다** (자주 걸리는 리뷰 포인트):
  - `MethodArgumentNotValidException` (Bean Validation, `@NotNull`/`@NotBlank` 등) → 400 `METHOD_ARGUMENT_NOT_VALID`
  - `HttpMessageNotReadableException` (JSON 파싱 실패) → 400 `INVALID_JSON_FORMAT`. 필수 필드 누락은 기본적으로 Bean Validation(`@NotNull` 등)으로 잡는 걸 권장한다. Request를 Java record로 만들고 생성자에서 값이 없어 Jackson이 예외를 던지는 경우도 Spring이 이 예외로 감싸서 전달하지만, 이 경로는 Bean Validation과 달리 필드별 `reason`을 주기 어렵다.
  - `NoResourceFoundException` (알 수 없는 URL) → 404 `NOT_FOUND`
  - `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException`, `ConstraintViolationException` → 400
  - `HttpRequestMethodNotSupportedException` → 405 `METHOD_NOT_ALLOWED`
  - 그 외 `ResponseEntityExceptionHandler`가 처리하는 나머지 예외는 `handleExceptionInternal`에서 500 `INTERNAL_SERVER_ERROR`로 공통 처리
- **필드별 검증 에러는 `violations: List<ValidationErrorResponse>`(`field`+`reason`)로 보고**한다. 여러 필드가 동시에 실패해도 각각 리포트된다.
- 4xx는 `warn`, 5xx는 `error`(스택트레이스 포함)로 로깅한다.

## 원칙

- `RuntimeException`을 직접 throw하지 않는다. 항상 `BusinessException`의 서브클래스(공통 5종 또는 도메인별 구체 예외)를 사용한다.
- **도메인 실패는 반드시 공통 5종 예외 또는 그 하위 도메인별 구체 예외로 던진다.** 맨 `throw`, `IllegalStateException` 등을 그대로 던지면 `GlobalExceptionHandler`의 catch-all이 잡긴 하지만 `INTERNAL_ERROR`(500)로 뭉개져서 원래 의도한 도메인 코드와 HTTP status를 잃는다.
- HTTP status는 **오직 `ErrorCode.getStatus()`를 통해서만** 표현한다. 핸들러나 예외 클래스에 하드코딩하지 않는다.
- 예외 메시지는 한국어로 작성한다.
- Controller 계층에서 비즈니스 예외를 직접 catch하지 않는다.
- Bean Validation 실패도 동일하게 `StandardResponse` 포맷으로 변환한다.
