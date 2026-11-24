# error-handling 결정 기록

예외 계층과 에러 코드의 근거, 택하지 않은 안, 변경 이력을 둔다. 현재 규칙은 `error-handling` 스킬이 단일 소스다. `error-handling` 스킬에 있던 결정 문단을 원문 그대로 옮겼다(2026-11-08, 이슈 #178).

## 예외 계층 구현 완료

**구현 완료 (2026-09-07, 이슈 #35)**: 아래 "예외 계층"(`error-handling` 스킬의 예외 계층 절, #178에서 변경)은 목표 설계가 아니라 실제 코드 상태다. `BusinessException.from(...)`은 더 이상 존재하지 않으며, `member`/`auth` 모두 공통 예외(`BadRequestException` 등)와 도메인별 구체 예외(`MemberNotFoundException`, `RefreshTokenNotFoundException` 등)를 실제로 쓰고 있다. 새 도메인을 스캐폴딩하거나 예외 관련 코드를 새로 작성할 때 `member`/`auth`의 기존 구체 예외 클래스를 그대로 참고 구현으로 삼아도 된다.

## InternalServerException 추가

**`InternalServerException` 추가 (2026-09-12)**: 처음에는 공통 예외가 4xx 5종(`BadRequestException`/`UnauthorizedException`/`ForbiddenException`/`NotFoundException`/`ConflictException`)뿐이었고, 500에 대응하는 공통 예외가 없어 `SocialAuthenticationFailedException`/`SocialMemberInfoFetchFailedException`이 `BusinessException`을 직접 상속했다. 여기에 `AccessTokenHashingFailedException`(체크 예외라 컴파일러가 강제로 잡게 만들지만 SHA-256처럼 실제로는 발생할 수 없는 JVM 불변조건 위반을 언체크로 바꾸는 방어적 분기)이 같은 패턴으로 세 번째 추가되면서, "예외적인 경우"라던 케이스 자체가 반복되는 패턴이 됐다(rule of three). 그래서 400/401/403/404/409와 동일하게 500도 공통 래퍼로 승격해 세 클래스 모두 `InternalServerException`을 상속하도록 통일했다.

## 토큰 해싱 유틸

**(2026-10-29 결정, 이슈 #123)**: 토큰 SHA-256 해싱을 `common.crypto.Sha256`으로 모았다. auth의 `RefreshTokenRepositoryImpl`, `BlacklistAccessTokenRepositoryImpl`과 gathering의 `GatheringJoinTokenRepositoryImpl`, `ParticipantLinkTokenRepositoryImpl`에 같은 private `hash()`가 네 벌 있었기 때문이다. 해싱 실패는 `InternalServerException(CommonErrorCode.HASHING_FAILED)`로 던진다. 그래서 `AccessTokenHashingFailedException`, `RefreshTokenHashingFailedException`, `GatheringTokenHashingFailedException`과 각 에러 코드를 지웠다. 모든 JDK는 SHA-256을 지원해야 해서 이 실패는 실제로 일어나지 않는다. 도메인마다 다른 에러 코드를 둘 이유가 없다. `Sha256`이 실패를 그대로 넘기고 호출부가 도메인 예외로 바꾸는 안은 택하지 않았다. 호출부마다 try/catch가 남아 중복이 줄지 않기 때문이다. 유틸은 `global`이 아니라 `common`에 둔다. `global`은 설정과 보안 필터를 두는 기술 부트스트랩이기 때문이다.

## 에러 코드 명명

**(2026-11-01 결정, 이슈 #156)**: `INVALID_` 접두사를 400 전용으로 정했다. `INVALID_GATHERING_MASTER_KEY`(500)는 설정을 읽는 작업의 실패라 `GATHERING_MASTER_KEY_LOAD_FAILED`로 바꿨다. `INVALID_TOP_LIMIT`(500)는 상수로만 들어오는 값을 검사하던 분기라 검사와 함께 지웠고, 에러 코드가 남지 않은 `PairingErrorCode`도 지웠다. `INVALID_JWT_TOKEN`(401)은 `UNAUTHENTICATED`로 합쳤다. 필터는 같은 상황을 이미 `UNAUTHENTICATED`로 응답했고, 이 코드는 로그아웃 중 토큰이 만료되는 경합에서만 나왔다. 실패 이유를 응답에서 구분하면 공격자가 위조 토큰이 서명 검증을 통과했는지 알 수 있고, 클라이언트가 할 수 있는 행동도 늘지 않는다. 그래서 실패 이유는 `JwtAccessTokenProvider`가 예외 이름만 debug 로그로 남긴다. 던지는 곳이 없던 `TOKEN_IS_BLACKLISTED`는 지웠다. `INVALID_`를 400과 401에 함께 쓰는 안은 택하지 않았다. 다른 401 코드가 `INVALID_`를 쓰지 않아 지금 코드와 덜 맞기 때문이다. `*_NOT_FOUND`를 404 전용으로 묶는 안도 택하지 않았다. `REFRESH_TOKEN_NOT_FOUND`는 클라이언트가 재로그인을 결정하는 코드라, 이름을 바꾸면 계약이 깨지기 때문이다.

## 500 폴백 로깅

2026-09-12에 `ValidationExceptionHandler.handleExceptionInternal`의 로깅을 `log.warn`에서 `log.error(..., ex)`로 바꿨다. 이전에는 500 폴백 경로의 스택트레이스가 남지 않았다.

## 검증 실패 로그

**(2026-10-29 결정, 이슈 #123)**: `MethodArgumentNotValidException` 로그에는 예외 메시지 대신 실패한 필드 이름만 남긴다. 예외 메시지에는 거부된 입력값이 그대로 들어 있어, 이름 같은 개인정보가 로그에 남기 때문이다.

## 503 공통 예외

**(2026-11-15 결정, 이슈 #182)**: 공통 예외에 `ServiceUnavailableException`(503)을 추가했다. 맛집 검색이 기대는 네이버 지역 검색은 장애가 나거나 하루 호출 한도를 넘으면 요청을 받지 못한다. 이때 클라이언트가 해야 할 일은 "잠시 후 다시 시도"이고, 페어링 화면은 그대로 쓸 수 있어야 한다. 500은 서버의 버그처럼 읽히므로 외부 서비스를 쓸 수 없다는 사실을 나타내지 못한다. 그래서 상태 코드로 구분한다. 500 아래에 에러 코드만 나누는 안은 택하지 않았다. 장애와 한도 초과가 모두 500으로 보여 모니터링에서 서버 버그와 섞이기 때문이다. 한도 초과도 429가 아니라 503으로 둔다. 429는 요청한 클라이언트의 호출이 많다는 뜻인데, 이 한도는 서비스 전체가 함께 쓰는 외부 한도라 개별 클라이언트가 줄여도 풀리지 않는다. 이름은 `*_UNAVAILABLE`과 `*_LIMIT_EXCEEDED`로 정했고, 400의 `*_EXCEEDS_*`와 구분된다.

**(2026-11-22 결정, 이슈 #223)**: 소셜 로그인의 검증과 사용자 정보 조회 실패도 외부 서비스를 지금 쓸 수 없는 경우라 `ServiceUnavailableException`을 상속하도록 바꿨다. 근거와 한계는 `auth.md`에 있다.

**(2026-11-24 결정, 이슈 #233)**: 행 잠금 대기 시간 초과와 데드락(`PessimisticLockingFailureException`)을 `GlobalExceptionHandler`가 503 `PESSIMISTIC_LOCKING_FAILURE`로 바꾼다. 다른 요청이 잠금을 쥐고 있어 실패한 것이라 요청을 다시 보내면 성공할 수 있다. 500은 서버의 버그처럼 읽혀 재시도할 수 있다는 사실을 나타내지 못한다. 409로 두는 안은 택하지 않았다. 409는 요청한 대상의 상태가 요청과 맞지 않다는 뜻이고, 이 경우는 대상의 상태가 아니라 처리하는 쪽의 사정으로 실패한 것이다. 코드 이름은 프레임워크 예외를 매핑한 코드의 규칙에 따라 예외 이름을 따랐다. 핸들러는 `ValidationExceptionHandler`가 아니라 `GlobalExceptionHandler`에 둔다. 이 예외는 스프링 웹 계층이 던지는 예외가 아니라 서비스 안의 DB 호출에서 나오고, `BusinessException`이 아니라서 어느 쪽에도 없던 분기이므로 서비스 안에서 나오는 실패를 담당하는 쪽에 맞췄다. 로그는 `BusinessException`처럼 `warn`에 스택트레이스 없이 남기되 잠금 대기 초과와 데드락을 구분하도록 예외 클래스명을 찍는다.

## 프레임워크 예외 폴백

**(2026-11-24 결정, 이슈 #232)**: `ValidationExceptionHandler.handleExceptionInternal`이 상태와 관계없이 본문을 `INTERNAL_SERVER_ERROR`로 만들어, Content-Type이 틀린 요청이 HTTP 415인데 본문 `status`는 500이었다. 클라이언트 오류가 서버 오류로 보이고 ERROR 로그로 남았다. 이제 본문 `status`는 응답 상태를 따르고, 코드는 406 `NOT_ACCEPTABLE`, 415 `UNSUPPORTED_MEDIA_TYPE`, 그 외 4xx `INVALID_REQUEST`, 5xx `INTERNAL_SERVER_ERROR`다. 4xx는 예외 메시지에 거부된 입력값이 들어 있을 수 있어 예외 클래스명과 상태만 `warn`으로 남긴다. 상태마다 `ErrorCode`를 모두 늘리는 안은 택하지 않았다. 전용 핸들러가 없는 예외는 자주 나오지 않으므로 상태를 구분하는 코드는 실제로 나오는 406과 415만 두고 나머지는 범용 코드로 묶는다. 그래서 그 외 4xx와 5xx의 코드는 `getStatus()`가 가리키는 상태와 본문 `status`가 다를 수 있다. 본문 `status`는 항상 응답 상태를 따른다.

## 지정 금액 필수화

**(2026-11-24 결정, 이슈 #232)**: `FixedAmountRequest.amount`는 원시 `long`이라 필드가 빠지면 0으로 읽혀 0원 분담금이 조용히 만들어졌다. `@NotNull Long`으로 바꿔 누락을 400으로 거절한다. `totalAmount`도 원시 `long`이지만 `@Positive`가 0을 거절하므로 누락이 400으로 걸러져 바꾸지 않았다.
