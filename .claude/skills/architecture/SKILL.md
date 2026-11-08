---
name: architecture
description: Load when designing layer structure. UseCase/Service/Api/Controller 분리, 계층별 타입 어휘, DTO↔Command 변환, 응답 포맷, 크로스 도메인 참조, 프로젝트 구조, 트랜잭션 경계, DDD 핵심 원칙, 인증 규칙, 영속성(PostgreSQL/pgvector, Flyway, 인덱스·제약, 프로파일 설정, 카탈로그 적재, persistence.md), pairing 추천 점수 규칙(pairing.md), gathering 정산 규칙(gathering.md), notification 발송 규칙(notification.md), 메시지 큐(RabbitMQ) 사용 범위.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Architecture Rules

이 문서는 현재 규칙만 담는다. 근거, 택하지 않은 안, 변경 이력은 `docs/decisions/`의 `architecture.md`, `auth.md`, `catalog.md`, `notification.md`에 있다. 규칙을 바꾸면 같은 커밋에서 두 곳을 함께 고친다(`design-decision` 스킬).

- 단일 Gradle 모듈에 도메인별 계층형 패키지를 둔다. 헥사고날 포트/어댑터 명칭과 멀티모듈 구조는 쓰지 않는다.
- `*Repository` 구현체는 `*RepositoryImpl`로 쓴다(예: `MemberRepositoryImpl implements MemberRepository`). `*RepositoryAdapter`는 금지한다.

## 프로젝트 구조

도메인은 `member`, `auth`, `catalog`, `pairing`, `gathering`, `notification` 6개이고, 각각 `domain`, `application`, `presentation`, `infrastructure` 패키지를 가진다.

| 도메인 | 책임 |
|---|---|
| `member` | 회원. 탈퇴 시 `application.event`의 `MemberDeletedEvent` 발행 |
| `auth` | 로그인, 재발급, 로그아웃(1 UseCase = 1 Service). Redis 기반 리프레시 토큰과 액세스 토큰 블랙리스트 |
| `catalog` | 음료(`Drink`, `DrinkStyle`), 안주(`Anju`), 음악 무드(`MusicMood`)와 그 맛, 분위기 값. 적재 규칙표, `*Reader`, 수동 적재 러너 `ImportCatalogRunner` |
| `pairing` | 자리(`Occasion`)에 맞는 술, 안주, 음악 조합 추천과 점수 계산, `Pairing`, `PairingFeedback`, 조합별 평점 집계 `PairingRatingSummary` |
| `gathering` | 모임, 참여, 정산 건과 분담금 상태 |
| `notification` | 정산 메시지의 발송 예약, 발송 기록, 큐 발행, 발송 구현체 |
| `common` | 공유 커널. 공통 예외(`common.exception`), `StandardResponse`, `BaseTimeEntity`, `@CommandService`/`@QueryService`(`common.annotation`) |
| `global` | 기술 부트스트랩. Security, JPA, Redis, Swagger 설정(`config`), 인증 필터와 핸들러(`security`) |

- 도메인 모델과 JPA 엔티티를 분리한다. Domain↔JPA 변환은 `*JpaEntity`의 `from(도메인객체)`와 `toDomain()`에 둔다. 변환에 암호화 키 같은 다른 값이 필요하면 인자로 받는다(예: `ParticipantJpaEntity.from(participant, gatheringId, dataKey)`). 별도 `*Mapper`는 두지 않는다.
- `shared` 패키지는 두지 않는다. 크로스 도메인 조율은 대상 도메인의 `*UseCase`를 직접 의존한다.

## 계층 의존 방향

```
presentation   ──→  application  ──→  domain
infrastructure ──→  application  ──→  domain   (infrastructure는 domain의 Repository 인터페이스를 구현)
```

- `domain`은 다른 계층에 의존하지 않는다. Spring, JPA를 포함한 프레임워크를 import하지 않는다. 예외는 에러 코드 enum의 `HttpStatus` 하나다.
- `domain`과 `application`은 `common`만 참조하고 `global`은 참조하지 않는다. `global`은 Spring 설정과 필터 wiring 목적으로만 `common`을 참조한다.
- `global`은 wiring 목적으로 도메인 `presentation`의 `*Endpoints`(URL 경로 문자열 상수 클래스)를 참조할 수 있다. `*Request`, `*Response`, `*Api`, `*Controller`는 참조하지 않는다.
- `*Endpoints`가 경로의 단일 소스다. `*Api`는 매핑 애노테이션에 같은 도메인의 `*Endpoints` 상수를 넣는다(예: `@PostMapping(AuthEndpoints.LOGIN)`). 클래스 레벨 `@RequestMapping`으로 베이스 경로를 나누지 않고, `*Endpoints`가 `BASE + 세부경로`로 완성한 전체 경로를 쓴다.
- `global.security`의 인증 필터와 인증 실패 핸들러는 `auth`의 `domain` 인터페이스(`AccessTokenProvider`, `BlacklistAccessTokenRepository`)와 에러 코드(`AuthErrorCode`)를 참조할 수 있다. 구현체와 `infrastructure`는 참조하지 않는다.
- 토큰 Provider도 `domain`에 인터페이스를 두고 `infrastructure`가 구현한다. 인터페이스가 기본 이름을 갖고, 구현체 이름에 방식을 붙인다(`JwtAccessTokenProvider`, `UuidRefreshTokenProvider`).

## 계층별 역할

| 계층 | 역할 |
|---|---|
| `domain` | 순수 도메인 모델, 도메인 서비스, `*Repository`·`*Reader` 인터페이스. 비즈니스 로직이 사는 곳 |
| `application` | `*UseCase` 인터페이스 + `*Service` 구현체(`@CommandService`/`@QueryService`). 오케스트레이션만 담당 |
| `presentation` | `*Controller`, `*Api` 인터페이스, `*Request`/`*Response`, 수동 실행 진입점 `*Runner`, 큐 리스너(`listener`), 스케줄러(`scheduler`) |
| `infrastructure` | `*RepositoryImpl`, `*JpaEntity`, Redis 등 외부 연동, 파일 원본을 읽는 `*Reader` 구현체 |

## 계층별 타입 어휘

각 계층은 자기 타입만 다루고, 경계를 넘을 때 명시적으로 변환한다. Service는 `*Request`를 받지 않고 도메인 엔티티나 `*JpaEntity`를 반환하지 않는다.

| 계층 | 입력 | 출력 | 변환 지점 |
|---|---|---|---|
| Presentation | `*Request` | `*Response` | `Request.toCommand()`/`toQuery()`, `*Response.from(Result)` |
| Application (조회) | 단일 식별자면 원시값, 그 외 `*Query` | `*Result`, 결과를 예외로만 알리는 조회는 `void` | `*UseCase` 시그니처 |
| Application (쓰기) | `*Command`, 내부 트리거가 식별자 하나만 넘기면 원시값 | `*Result`, 반환값이 필요 없으면 `void` | `*UseCase` 시그니처 |
| Domain | 원시값 | 도메인 객체 | 도메인 팩토리 메서드/생성자 |
| Infrastructure | 도메인 객체 | `*JpaEntity` | `*JpaEntity.from()`/`toDomain()` |

- `*Result`는 도메인 엔티티를 감싸지 않고 필드를 평탄화한다. 크로스 도메인 계약도 `*Result`다(예: `RefreshService`가 `member.GetMemberUseCase`에서 받는 `MemberResult`).
- DTO(`*Command`/`*Result`/`*Request`/`*Response`)는 자기 정적 팩토리(`from`/`of`)나 변환 메서드(`to*`)에서 필드를 채운다. 호출부가 `new`로 필드를 나열해 조립하지 않는다.
- 변환 대상이 다른 도메인 소속이면 이미 그 도메인을 의존하는 쪽이 변환 메서드를 갖는다(예: `auth.OAuthMemberResult.toMemberCreateCommand()`).
- 조회 파라미터가 2개 이상이거나 필터, 정렬, 페이징처럼 늘어날 조건이면 `*Query`로 감싼다.
- 스케줄러, 큐 리스너, 이벤트 리스너처럼 내부에서 시작하는 쓰기는 넘길 값이 식별자 하나면 원시값으로 받고(예: `dispatch(Long gatheringId)`), 2개 이상이면 `*Command`로 감싼다.

## 크로스 도메인 참조

- 크로스 도메인 조율은 Facade 없이 대상 도메인의 `*UseCase`와 `*Result`를 직접 의존한다. 다른 도메인의 Repository와 구현체는 참조하지 않는다.
- 다른 도메인에서 참조할 수 있는 것은 `application`의 `usecase`, `dto`, `event`, `provider`와 `domain`의 enum뿐이다. enum이 직접 선언한 메서드는 호출하지 않는다. `name()`, `valueOf()`, `values()` 같은 `java.lang.Enum` 기본 메서드는 호출할 수 있다. 테스트 코드에는 이 규칙을 적용하지 않는다.
- 하나의 `*Service`가 크로스 도메인 `*UseCase`를 2개 이상 의존하게 되면 그 UseCase를 쪼갤 수 있는지부터 검토한다. `SettlementCommandService`와 `ShareCommandService`가 `notification`의 UseCase 두 개씩에 의존하는 것은 검토를 마친 예외다.
- 다른 도메인이 선언한 인터페이스를 구현할 때도 Port, Adapter라는 이름은 쓰지 않는다.

### catalog와 pairing

- 의존 방향은 `pairing → catalog` 한 방향이다. 테이블과 FK는 도메인 분리 전 그대로다.
- `FlavorProfile`, `MoodProfile`은 두 도메인이 각자 가진다. `catalog`의 것은 적재 규칙이 계산한 값만, `pairing`의 것은 점수 계산 메서드를 가진다.
- 점수 계산 메서드와 추천 필터는 `pairing`의 `PairingDrink`, `PairingAnju`, `PairingMusicMood`에 둔다. 음식 계열(`Harmonize`) 판정은 `catalog`에 둔다.
- `catalog`의 UseCase는 `GetCatalogUseCase`(`CatalogQuery`의 도수 상한 조건으로 일괄 조회), `VerifyCatalogReferencesUseCase`(id로 참조한 대상이 모두 있는지 확인), `GetCatalogReferencesUseCase`(id로 가리킨 대상의 표시 이름 조회) 세 개이고, `CatalogQueryService` 하나가 함께 구현한다.
- `VerifyCatalogReferencesUseCase.verify`는 `CatalogReferencesQuery`를 받고 반환값이 없다. 없는 대상은 `DrinkNotFoundException`, `AnjuNotFoundException`, `MusicMoodNotFoundException`으로 알린다.
- `CatalogResult`는 추천에 필요한 값을 원시값과 enum으로 담는다. 음식 계열 판정은 계열마다 비트 하나인 `harmonizeMask`로 담고, `pairing`은 두 값의 비트 AND만 본다.
- `CatalogResult`를 `PairingDrink`, `PairingAnju`, `PairingMusicMood`로 바꾸는 코드는 `PairingQueryService`의 private 메서드에 둔다.
- 적재에서 빠진 안주를 지울 때 `AnjuRepository.findReferencedIds`가 `pairing_feedback`이 참조하는 안주를 네이티브 쿼리로 걸러 낸다.
- 타입 이름에 `Candidate`, `Item(s)`를 쓰지 않는다.

### gathering과 notification

- 의존 방향은 `gathering → notification` 한 방향이다.
- 정산 건 기록과 수정은 정산 건을 바꾸기 전에 `notification`의 예약 UseCase를 같은 트랜잭션에서 부른다. 취소 알림도 같은 트랜잭션에서 부른다.
- `notification`이 `gathering`의 데이터를 써야 하는 곳은 `notification.application.provider`에 인터페이스로 선언하고, `gathering.application.provider`의 `*ProviderImpl`이 구현한다. 주고받는 값은 `notification`의 `*Result`다.
- 메시지에 담을 금액을 고르는 규칙(미입금 분담금, 취소된 정산 건)은 `gathering`에 둔다.
- 설정 키와 큐 이름의 접두사는 `notification.*`이다. 링크 주소 환경 변수 `GATHERING_LINK_BASE_URL`과 테이블 이름, FK는 바꾸지 않는다.

## 크로스 도메인 부수효과

- 부수효과가 실패했을 때 원본 작업도 롤백돼야 하면 각 도메인 `*UseCase`를 같은 흐름에서 직접 호출한다.
- 원본이 커밋된 뒤에만 의미가 있고 실패해도 원본을 되돌릴 이유가 없는 후속 정리라면 `@TransactionalEventListener(phase = AFTER_COMMIT)` 도메인 이벤트로 분리한다(예: 회원 삭제 뒤 로그아웃 처리).
- 이벤트 이름은 `<Entity><과거분사>Event`(예: `MemberDeletedEvent`)다. 구독자에게 필요한 요청 컨텍스트(accessToken, ip 등)도 담을 수 있다.
- 이벤트는 발행하는 도메인의 `application.event`에 정의하고, `@CommandService` 구현체 안에서 `ApplicationEventPublisher.publishEvent(...)`로 발행한다. 트랜잭션 밖에서 발행하면 `AFTER_COMMIT` 리스너가 걸리지 않는다.
- 구독은 구독하는 도메인의 `application.listener`에 `@Component` 클래스로 두고, 대상 도메인의 `*UseCase`에 위임만 한다.
- 공유 마커 인터페이스나 베이스 클래스(`common.event` 등)는 두지 않는다.

## 트랜잭션 / CQRS

- 모든 `*Service`에 `@CommandService`(`@Service` + `@Transactional`) 또는 `@QueryService`(`@Service` + `@Transactional(readOnly = true)`)를 붙인다.
- 메서드 단위 `@Transactional`은 붙이지 않는다. 한 클래스는 한 가지 트랜잭션 성격만 갖는다. 성격이 섞이면 UseCase를 나눈다.
- `spring.jpa.open-in-view=false`다. Controller에서 지연 로딩을 쓰지 않는다.

## 하위 문서

| 문서 | 읽는 때 |
|---|---|
| [`persistence.md`](persistence.md) | 엔티티와 `*JpaEntity` 매핑, `db/migration`, `application*.yaml`, 적재 러너나 적재 규칙표를 바꿀 때. 맛 값을 계산하는 규칙표(`FlavorRule`, `WineFlavorRule`, `AnjuRule`)와 `Drink`, `DrinkStyle` 분리도 여기 있다 |
| [`pairing.md`](pairing.md) | `Pairing.recommend`의 점수 규칙, 후보 선정, 추천 성능, 피드백 집계를 바꿀 때. Harmonize 규칙표(`wine-harmonize.csv`, `beer-harmonize.csv`), `drink.harmonizes` 컬럼, 안주 무게감 보정 규칙표(`anju-serving-richness.csv`)도 여기 있다 |
| [`gathering.md`](gathering.md) | `gathering`의 모델, 정산 흐름, 개인정보 암호화와 파기, 익명 페어링 피드백을 바꿀 때. `pairing_feedback.member_id`처럼 `gathering` 때문에 바뀌는 `pairing` 규칙도 여기 있다 |
| [`notification.md`](notification.md) | 발송 예약, 발송 기록, 재시도, 발송 시간대, 개인 링크, 큐 발행과 컨슈머, 폴러를 바꿀 때. 정산 건 변경이 예약과 취소 알림에 미치는 영향도 함께 읽는다 |

## 메시지 큐 (RabbitMQ)

- 큐는 정산 메시지와 리마인드 발송에만 쓰고, 발송은 `notification`이 맡는다.
- 발송 예정 시각은 DB의 예약이 갖는다. 큐는 수신자별 발송, 재시도, DLQ, 외부 장애 격리를 맡는다. 지연을 큐에 맡기지 않는다.
- 메시지에는 id만 담는다. 컨슈머는 발송 직전에 상태를 다시 조회하고 멱등하게 처리한다.
- 리마인드 횟수와 간격은 큐 설정이 아니라 `NotificationPolicy`가 정한다.
- 개인정보 파기는 주기적 스윕으로, 페어링 피드백 집계는 큐 없이 처리한다.
- 다른 곳에 큐를 쓰려면 다음 중 하나에 해당해야 한다. 예약이 시각별로 흩어진다. 재시작해도 예약이 유실되면 안 된다. 외부 장애를 격리하고 재시도와 DLQ가 필요하다.

## DDD 핵심 원칙

- 도메인 로직은 도메인 객체 메서드에 둔다. Service는 오케스트레이션만 한다.
- `@Service`는 `*UseCase` 구현체에만 붙인다. `@Component`는 스케줄러, 이벤트 리스너 같은 기술 wiring에만 쓰고, 여러 도메인을 조율하는 용도로 쓰지 않는다.
- 1 `*UseCase` = 1 `*Service`가 기본이다. 부수효과, 외부 연동 없는 유사한 단순 조회와 CRUD는 Query/Command 레벨로 묶을 수 있다(예: `MemberQueryService`, `MemberCommandService`). 트리거, 부수효과, 외부 연동이 다른 흐름은 UseCase 단위로 나눈다(예: `RefreshService`, `LogoutService`).
- Request DTO의 `toCommand()`로 Command를 만들고, 도메인은 원시값만 받는다.
- 인증 식별자는 `@AuthenticationPrincipal String memberId`로 받는다.
- 안 쓰는 에러 코드, 검증기, 추상화는 제거한다(YAGNI).

## 인증

- 리프레시 토큰은 RTR로 회전한다. 탈취를 감지하고, 누락되면 거부한다(Tombstone 없음).
- 리프레시 토큰은 httpOnly, Secure, `SameSite=Strict` 쿠키로, 액세스 토큰은 응답 바디로 전달한다. 쿠키 `Path`는 `AuthEndpoints.BASE`다.
- CORS는 `Access-Control-Allow-Credentials: true`라서 허용 오리진을 명시한다. 와일드카드(`*`)는 쓰지 않는다. CSRF는 `SameSite=Strict`로 막고 별도 CSRF 토큰은 두지 않는다.
- 로그아웃은 인증 없이 호출할 수 있고, 토큰이 없거나 만료돼도 204를 반환한다. 쿠키의 리프레시 토큰 하나를 `consume`으로 지우고, 필터가 인증한 유효한 액세스 토큰만 블랙리스트에 올린다.
- 회원 탈퇴는 `MemberDeletedEventListener`가 `ForceRevokeUseCase`로 리프레시 토큰을 모두 지우고 `LogoutUseCase`로 액세스 토큰을 블랙리스트에 올린다.
- `CustomAuthenticationEntryPoint`는 401 응답에 `WWW-Authenticate: Bearer realm="symphonia"`를 넣는다. `GlobalExceptionHandler`의 401(`REFRESH_TOKEN_NOT_FOUND`)에는 넣지 않는다.
- 앱 단 rate limit은 두지 않는다. 대량 요청 방어는 LB/WAF가 맡는다.
- 클라이언트 IP는 감사 로그(`AuditLogRecorder`)에만 쓴다. `server.forward-headers-strategy: native`와 `server.tomcat.remoteip.internal-proxies`(`TRUSTED_PROXIES`)로 신뢰 프록시의 `X-Forwarded-For`만 반영하고, 어디서든 `HttpServletRequest.getRemoteAddr()`로 구한다. 별도 IP 추출 유틸과 테스트는 두지 않는다.

## 응답 포맷

`StandardResponse<T>`(`common.response`)가 모든 응답을 감싼다. 필드는 `boolean ok`, `int status`, `T data`다. 성공은 `StandardResponse.success(status, data)`(반환값 없으면 `data` 없는 오버로드), 에러는 `StandardResponse.fail(status, ErrorResponse)`로 만든다.

## 계층 규칙 검증 범위

- import와 선언만 보고 판정할 수 있는 계층 규칙은 `.claude/scripts/check-architecture.sh`가 grep으로 검사한다. `check-all.sh`가 이 스크립트를 실행한다. 검사 항목은 스크립트가 단일 소스다.
- 판단이 필요한 규칙(절차적 도메인 로직, 다른 도메인 enum의 메서드 호출, DTO 직접 조립, `*Result`의 엔티티 노출 등)은 `code-reviewer` 에이전트가 검토한다.
- Konsist, ArchUnit 같은 아키텍처 검증 도구는 도입하지 않는다.
