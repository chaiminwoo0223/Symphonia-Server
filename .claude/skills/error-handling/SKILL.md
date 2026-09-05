---
name: error-handling
description: Load when handling exceptions/errors. AppException + ResponseCode, 공통/도메인 예외, GlobalExceptionHandler, StandardResponse 에러 포맷, Bean Validation 변환.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Error-Handling Rules

## 구성 요소

| 요소 | 위치 | 역할 |
|---|---|---|
| `ResponseCode` (인터페이스) | `common.exception` | `code`·`message`·`status`(HTTP) 계약. 성공/에러 코드 모두 구현 |
| `AppException` (추상) | `common.exception` | 모든 비즈니스 예외의 베이스. `errorCode: ResponseCode`를 갖는다 |
| 공통 예외 (`NotFoundException` 등) | `common.exception` | 의미별 얇은 래퍼: `BadRequest`/`Unauthorized`/`Forbidden`/`NotFound`/`Conflict` |
| 도메인 `*ErrorCode` (enum) | 각 도메인의 `domain` 패키지 | `ResponseCode` 구현. 도메인별 코드 정의 |
| `CommonErrorCode` (enum) | `common.exception` | 도메인 무관 공통 에러 코드 |
| `GlobalExceptionHandler` | `common.exception.handler` | `@RestControllerAdvice`. 모든 예외를 `StandardResponse`로 변환 |

Symphonia는 단일 모듈이라 todakun처럼 `common-web` 모듈을 따로 두지 않는다. `GlobalExceptionHandler`도 그냥 `common`에 둔다.

## 예외 계층

```
AppException (common, errorCode: ResponseCode)
├── BadRequestException / UnauthorizedException / ForbiddenException / NotFoundException / ConflictException (common)
│     └── MemberNotFoundException 등: 도메인별 구체 예외 ({domain}/domain)
└── (도메인이 AppException을 직접 상속할 수도 있음)
```

## 코드 정의 (도메인별 `*ErrorCode`)

도메인 코드는 `ResponseCode`를 구현하는 enum으로 정의한다 (코드 자체가 HTTP status를 갖는다).

```java
// member/domain
public enum MemberErrorCode implements ResponseCode {
    MEMBER_NOT_FOUND("MEMBER-404", "회원을 찾을 수 없습니다", 404),
    DUPLICATE_NICKNAME("MEMBER-409", "이미 사용 중인 닉네임입니다", 409);

    private final String code;
    private final String message;
    private final int status;

    MemberErrorCode(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    // getter 생략
}

public class MemberNotFoundException extends NotFoundException {
    public MemberNotFoundException() {
        super(MemberErrorCode.MEMBER_NOT_FOUND);
    }
}
```

## GlobalExceptionHandler

`GlobalExceptionHandler`(`@RestControllerAdvice`)는 애플리케이션 전체에 **하나만** 둔다.

- **`AppException`만 캐치**하고 `errorCode.status`로 HTTP status를 결정한다 (예외 타입별 분기 불필요).
- 프레임워크 예외는 `CommonErrorCode`로 매핑하되, **클라이언트 잘못을 500으로 흘리지 않는다** (자주 걸리는 리뷰 포인트):
  - `MethodArgumentNotValidException` (Bean Validation, `@NotNull`/`@NotBlank` 등) → 400 `VALIDATION_ERROR`
  - `HttpMessageNotReadableException` (JSON 파싱 실패) → 400. 필수 필드 누락은 기본적으로 Bean Validation(`@NotNull` 등)으로 잡는 걸 권장한다. Request를 Java record로 만들고 생성자에서 값이 없어 Jackson이 예외를 던지는 경우도 Spring이 이 예외로 감싸서 전달하지만, 이 경로는 Bean Validation과 달리 필드별 `reason`을 주기 어렵다.
  - `NoResourceFoundException` (알 수 없는 URL) → 404 `NOT_FOUND`
  - `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException` → 400
- **필드별 검증 에러는 `reason: Map<String, String>`으로 보고**(필드 → 메시지) 한다. 여러 필드가 동시에 실패해도 각각 리포트된다. **단, `StandardResponse<T>`의 최종 필드 구성(`ok`/`data` 외 추가 필드)이 아직 미확정이라 `reason` 필드의 정확한 위치는 그 결정 이후 확정한다** (`architecture` 스킬의 "응답 포맷" 열린 항목과 연결).
- 4xx는 `warn`, 5xx는 `error`(스택트레이스 포함)로 로깅한다.

## 원칙

- `RuntimeException`을 직접 throw하지 않는다. 항상 `AppException`(또는 공통/도메인 하위 클래스)을 사용한다.
- **도메인 실패는 반드시 `AppException`으로 던진다.** 맨 `throw`, `IllegalStateException` 등을 그대로 던지면 `GlobalExceptionHandler`의 catch-all이 잡긴 하지만 `INTERNAL_ERROR`(500)로 뭉개져서 원래 의도한 도메인 코드와 HTTP status를 잃는다.
- HTTP status는 **오직 `ResponseCode.status`를 통해서만** 표현한다. 핸들러에 하드코딩하지 않는다.
- 예외 메시지는 한국어로 작성한다.
- Controller 계층에서 비즈니스 예외를 직접 catch하지 않는다.
- Bean Validation 실패도 동일하게 `StandardResponse` 포맷으로 변환한다 (`code`는 `CommonErrorCode.VALIDATION_ERROR`).
