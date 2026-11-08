# error-handling 결정 기록

예외 계층과 에러 코드의 근거, 택하지 않은 안, 변경 이력을 둔다. 현재 규칙은 `error-handling` 스킬이 단일 소스다. `error-handling` 스킬에 있던 결정 문단을 원문 그대로 옮겼다(2026-11-08, 이슈 #178).

## 예외 계층 구현 완료

**구현 완료 (2026-09-07, 이슈 #35)**: 아래 "예외 계층"은 목표 설계가 아니라 실제 코드 상태다. `BusinessException.from(...)`은 더 이상 존재하지 않으며, `member`/`auth` 모두 공통 예외(`BadRequestException` 등)와 도메인별 구체 예외(`MemberNotFoundException`, `RefreshTokenNotFoundException` 등)를 실제로 쓰고 있다. 새 도메인을 스캐폴딩하거나 예외 관련 코드를 새로 작성할 때 `member`/`auth`의 기존 구체 예외 클래스를 그대로 참고 구현으로 삼아도 된다.

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
