---
name: architecture
description: Load when designing layer structure. UseCase/Service/Api/Controller 분리, 계층별 타입 어휘, DTO↔Command 변환, 응답 포맷, 크로스 도메인 참조, 프로젝트 구조, 트랜잭션 경계, DDD 핵심 원칙.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Architecture Rules

Symphonia는 **단일 Gradle 모듈 + 도메인별 계층형 패키지** 구조다. 헥사고날 포트/어댑터 명칭, 멀티모듈 구조는 사용하지 않는다. `*Repository` 구현체를 `*RepositoryAdapter`로 명명하는 것도 금지된 포트/어댑터 명칭이다. 반드시 `*RepositoryImpl`을 쓴다 (예: `MemberRepositoryImpl implements MemberRepository`).

## 프로젝트 구조

```
com.symphonia
├── member/
│   ├── domain/            # 순수 도메인 모델(Member), 도메인 서비스, MemberRepository 인터페이스
│   ├── application/       # *UseCase 인터페이스 + *Service 구현체, Command (예: MemberQueryService, MemberCommandService, Query/Command 레벨)
│   ├── presentation/      # Controller, *Api 인터페이스, Request/Response DTO
│   └── infrastructure/    # MemberRepository 구현체, MemberJpaEntity, Domain↔JPA 매퍼
├── auth/
│   ├── domain/
│   ├── application/       # *UseCase 인터페이스 + *Service 구현체, 1 UseCase = 1 Service (예: RefreshUseCase/RefreshService, LogoutUseCase/LogoutService)
│   ├── presentation/
│   └── infrastructure/    # Redis 기반 RefreshToken/BlacklistAccessToken 구현, JPA 엔티티
└── common/                # 공통 예외(AppException 등), 응답 포맷(StandardResponse), CQRS 트랜잭션 애노테이션(@CommandService/@QueryService, common.annotation), 설정
```

> 도메인 모델과 JPA 엔티티는 완전히 분리한다. `infrastructure`에 `*JpaEntity` + Domain↔JPA 매퍼를 두고, `domain`은 영속성 기술을 전혀 알지 못한다.
> 현재 도메인은 2개(`member`, `auth`)뿐이라 별도 `shared` 패키지는 아직 두지 않는다. 크로스 도메인 조율은 대상 도메인의 `*UseCase` 인터페이스를 직접 의존하는 것으로 충분하다. 3번째 도메인이 늘거나, 하나의 `*Service`가 크로스 도메인 `*UseCase`를 2개 이상 의존하게 되는 시점에 재검토한다.

## 계층 의존 방향

```
presentation   ──→  application  ──→  domain
infrastructure ──→  application  ──→  domain   (infrastructure는 domain의 Repository 인터페이스를 구현)
```

`domain`은 다른 어떤 계층에도 의존하지 않는다. Spring, JPA를 포함한 프레임워크 임포트를 전면 금지한다. `domain → infrastructure` 참조는 금지된 방향이다.

## 계층별 역할

| 계층 | 역할 |
|---|---|
| `domain` | 순수 도메인 모델, 도메인 서비스, `*Repository` 인터페이스. 비즈니스 로직이 실제로 사는 곳 |
| `application` | `*UseCase` 인터페이스 + `*Service` 구현체(`@CommandService`/`@QueryService`). 오케스트레이션만 담당 |
| `presentation` | `*Controller`, `*Api` 인터페이스, `*Request`/`*Response` DTO |
| `infrastructure` | `*RepositoryImpl`(`*Repository` 구현체), `*JpaEntity`, Domain↔JPA 매퍼, Redis 등 외부 연동 |

## 계층별 타입 어휘 (Layer Type Vocabulary)

각 계층은 자기 타입만 다루고, 계층 경계를 넘을 땐 반드시 명시적 변환을 거친다. Service가 `*Request`를 직접 받거나 도메인 엔티티·`*JpaEntity`를 그대로 반환하는 일은 없다.

| 계층 | 입력 | 출력 | 변환 지점 |
|---|---|---|---|
| Presentation | `*Request` | `*Response` | `Request.toCommand()`/`toQuery()`, `*Response.from(Result)` |
| Application (조회) | `*Query` (파라미터 개수와 무관하게 항상 사용) | `*Result` | `*UseCase` 인터페이스 시그니처 |
| Application (쓰기) | `*Command` | `*Result` 또는 `void`(반환값이 불필요한 경우, 예: 삭제) | `*UseCase` 인터페이스 시그니처 |
| Domain | 원시값 | 도메인 객체 | 도메인 팩토리 메서드/생성자 |
| Infrastructure | 도메인 객체 | `*JpaEntity` | Domain↔JPA 매퍼 |

- `*Result`는 도메인 엔티티를 감싸지 않는다. 도메인 필드를 평탄화한 별도 DTO로 만들어 도메인 객체가 Application 계층 밖으로 새어나가지 않게 한다. **크로스 도메인으로 주고받을 때도 이 `*Result`가 그대로 계약 역할을 한다.** 예를 들어 `RefreshService`가 `member.GetMemberUseCase`를 호출해 받는 것도 `MemberResult`다.
- 조회(Query)는 파라미터가 하나뿐이어도 항상 `*Query` 객체로 감싼다 (계층 응집도·일관성을 YAGNI보다 우선).
- 반환값이 필요 없는 Command(삭제 등)는 `void`를 허용한다. 빈 `*Result`를 억지로 만들지 않는다.

## 크로스 도메인 참조 예시

- 크로스 도메인 조율이 필요한 `*Service`는 별도 Facade 없이, 대상 도메인의 `*UseCase` 인터페이스(및 `*Result`)를 직접 의존한다. Repository·구현체 직접 참조는 금지.
- 예: `auth/application/RefreshService`는 Member 역할 조회를 위해 `member.GetMemberUseCase`를 직접 의존한다.
- **가드레일**: 하나의 `*Service`가 크로스 도메인 `*UseCase`를 2개 이상 의존해야 하는 상황이 오면, Facade를 다시 두는 대신 그 UseCase 자체가 너무 커진 신호로 보고 쪼갤 수 있는지부터 검토한다.

## 트랜잭션 / CQRS

`@CommandService`(`@Service` + `@Transactional`), `@QueryService`(`@Service` + `@Transactional(readOnly = true)`) 메타 애노테이션을 `common.annotation`에 두고 모든 `*Service`에 부착한다.

- 메서드 단위 `@Transactional`은 개별 부착하지 않는다. 클래스 하나 = 트랜잭션 성격 하나가 원칙이다. 이게 지켜지지 않으면 그 자체로 UseCase가 여러 책임을 겸하고 있다는 신호다.
- `spring.jpa.open-in-view=false` (OSIV 비활성화). Controller에서 지연 로딩 사용 금지.

## DDD 핵심 원칙

- **DDD 기반**: 도메인 로직은 도메인 객체 메서드에 위치 (Service는 오케스트레이션만).
- **`@Service` vs `@Component`**: `@Service`는 `*UseCase` 구현체(비즈니스 로직 실행자)에 붙인다. `@Component`는 스케줄러·이벤트 리스너 같은 기술적 wiring에만 쓴다. 여러 도메인을 조율하는 비즈니스 오케스트레이터 용도로는 쓰지 않는다.
- **UseCase 단위 분리 (Facade 대체)**: 1 `*UseCase` 인터페이스 = 1 `*Service` 구현체가 기본. 로그인/재발급/로그아웃처럼 트리거·부수효과가 다른 흐름을 하나의 Service가 묶어 처리하지 않는다.
  - **판단 기준**: 메서드들이 부수효과·외부 연동 없이 유사한 단순 조회/CRUD라면 Query/Command 레벨 유지 가능 (예: `MemberQueryService`/`MemberCommandService`). 트리거·부수효과·외부 연동이 서로 다른 별개의 흐름이라면 UseCase 단위로 반드시 분리 (예: `RefreshUseCase`/`RefreshService`, `LogoutUseCase`/`LogoutService`).
- **Presentation → Application 변환**: Request DTO의 `toCommand()`로 Command 객체 생성, 도메인은 원시값만 받음.
- **인증 식별자 추출**: `@AuthenticationPrincipal String memberId`.
- **RTR(Refresh Token Rotation)**: 토큰 탈취 감지, 누락 시 단순 거부(Tombstone 없음). 리프레시 토큰은 httpOnly+Secure+SameSite 쿠키로 전달, 액세스 토큰은 응답 바디.
- **쿠키 기반 인증 제약**: CORS `Access-Control-Allow-Credentials: true` 필요 → Origin 와일드카드(`*`) 사용 불가, 허용 오리진 명시 필수. CSRF는 리프레시 토큰 쿠키에 `SameSite=Strict` 적용으로 방어(별도 CSRF 토큰 없음). 쿠키 `Path`는 재발급 엔드포인트로 제한 권장.
- **YAGNI**: 안 쓰는 에러 코드·검증기·추상화는 주저 없이 제거.

## 응답 포맷 (미확정, 열린 항목)

`StandardResponse<T>`가 모든 응답을 감싸며 `boolean ok`, `T data`를 기본 필드로 갖는다. **추가 필드(에러 코드, 타임스탬프 등) 구성은 아직 확정되지 않았다.** 이 부분을 다루는 작업 전에 먼저 사용자에게 확인한다.

## 정적분석 도구 미도입 사유

Konsist/ArchUnit 같은 아키텍처 자동검증 도구는 도입하지 않기로 했다. 단일 모듈 구조라 패키지 규칙 위반을 코드리뷰(`code-reviewer` 에이전트의 체크리스트 기반 수동 검토)로 잡는 게 더 실용적이라고 판단했기 때문이다. 계층 의존 방향처럼 판단이 섞이는 규칙은 이 방식으로 검증하고, EOF 개행·코드 스타일처럼 완전히 결정론적인 것만 스크립트/포맷터로 강제한다.
