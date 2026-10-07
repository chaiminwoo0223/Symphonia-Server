---
name: architecture
description: Load when designing layer structure. UseCase/Service/Api/Controller 분리, 계층별 타입 어휘, DTO↔Command 변환, 응답 포맷, 크로스 도메인 참조, 프로젝트 구조, 트랜잭션 경계, DDD 핵심 원칙, 영속성(PostgreSQL/pgvector, 인덱스·제약, compose/DB 설정).
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Architecture Rules

Symphonia는 **단일 Gradle 모듈 + 도메인별 계층형 패키지** 구조다. 헥사고날 포트/어댑터 명칭, 멀티모듈 구조는 사용하지 않는다. `*Repository` 구현체를 `*RepositoryAdapter`로 명명하는 것도 금지된 포트/어댑터 명칭이다. 반드시 `*RepositoryImpl`을 쓴다 (예: `MemberRepositoryImpl implements MemberRepository`).

## 프로젝트 구조

```
com.symphonia
├── member/
│   ├── domain/            # 순수 도메인 모델(Member), 도메인 서비스, MemberRepository 인터페이스
│   ├── application/       # *UseCase 인터페이스 + *Service 구현체, Command (예: MemberQueryService, MemberCommandService, Query/Command 레벨), event(도메인 이벤트, 예: MemberDeletedEvent)
│   ├── presentation/      # Controller, *Api 인터페이스, Request/Response DTO
│   └── infrastructure/    # MemberRepository 구현체, MemberJpaEntity(Domain↔JPA 변환은 from()/toDomain() 정적 팩토리 메서드)
├── auth/
│   ├── domain/
│   ├── application/       # *UseCase 인터페이스 + *Service 구현체, 1 UseCase = 1 Service (예: RefreshUseCase/RefreshService, LogoutUseCase/LogoutService), listener(다른 도메인 이벤트 구독, 예: MemberDeletedEventListener)
│   ├── presentation/
│   └── infrastructure/    # Redis 기반 RefreshToken/BlacklistAccessToken 구현, JPA 엔티티
├── pairing/               # 술, 안주, 음악 페어링 추천 (이슈 #45)
│   ├── domain/            # 순수 도메인 모델(Drink, DrinkStyle, Anju, MusicMood, Pairing), FlavorProfile/MoodProfile/Occasion 값 객체, PairingFeedback, *Repository 인터페이스, 규칙표로 맛을 계산하는 FlavorRule, 적재 원본을 읽는 *Reader 인터페이스(reader)
│   ├── application/       # *UseCase 인터페이스 + *Service 구현체 (예: PairingQueryService, PairingCommandService, Query/Command 레벨. 파일을 읽는 적재는 UseCase 단위로 분리: ImportDrinkStyleService, ImportCuratedDrinkService)
│   ├── presentation/      # Controller, *Api, 수동 적재 진입점 ImportDrinkRunner(runner)
│   └── infrastructure/    # DrinkRepository/AnjuRepository/PairingFeedbackRepository 구현체, JPA 엔티티, *Reader 구현체(reader: JsonBjcpStyleReader, CsvFlavorRuleReader, CsvCuratedDrinkReader)
├── common/                # 공유 커널: 공통 예외(BusinessException 등, common.exception), 응답 포맷(StandardResponse), BaseTimeEntity, CQRS 트랜잭션 애노테이션(@CommandService/@QueryService, common.annotation)
└── global/                # 기술 부트스트랩: Security/JPA/Redis/Swagger 설정 (config), 인증 필터·핸들러 (security)
```

> 도메인 모델과 JPA 엔티티는 완전히 분리한다. `domain`은 영속성 기술을 전혀 알지 못한다. Domain↔JPA 변환은 기본적으로 `*JpaEntity`의 정적 팩토리 메서드(`from(도메인객체)`, `toDomain()`)로 처리한다. 같은 변환을 여러 곳에서 재사용하거나 필드 매핑이 단순 대입을 넘어 계산·검증을 포함하게 되는 시점에만 별도 `*Mapper` 클래스로 분리한다 (YAGNI: 호출자가 하나뿐인 단순 매핑에 별도 클래스를 미리 만들지 않는다).
> 도메인은 총 3개(`member`, `auth`, `pairing`)다. 3번째 도메인이 늘어난 시점에 `shared` 패키지 도입 여부를 재검토했다. `pairing`은 아직 `member`나 `auth`의 `*UseCase`에 의존하지 않아 별도 `shared` 패키지를 두지 않기로 했다. 크로스 도메인 조율은 대상 도메인의 `*UseCase` 인터페이스를 직접 의존하는 것으로 충분하다. 앞으로 `pairing`이 회원 정보를 반영하는 등 크로스 도메인 의존이 생기고, 그중 하나의 `*Service`가 크로스 도메인 `*UseCase`를 2개 이상 의존하게 되는 시점에 다시 재검토한다.

## 계층 의존 방향

```
presentation   ──→  application  ──→  domain
infrastructure ──→  application  ──→  domain   (infrastructure는 domain의 Repository 인터페이스를 구현)
```

`domain`은 다른 어떤 계층에도 의존하지 않는다. Spring, JPA를 포함한 프레임워크 임포트를 전면 금지한다. `domain → infrastructure` 참조는 금지된 방향이다.

`domain`/`application` 계층은 공유 커널인 `common`만 참조하고, 기술 부트스트랩인 `global`(config/security)은 참조하지 않는다. `global`은 오직 Spring 설정·필터 wiring 목적으로만 `common`을 참조할 수 있다.

`global`은 같은 이유로 도메인의 `presentation` 계층에 있는 `*Endpoints`(비즈니스 로직·프레임워크 의존 없이 URL 경로 문자열만 담은 상수 클래스, 예: `auth.presentation.AuthEndpoints`)도 참조할 수 있다. 이는 `SecurityConfig`의 permitAll 매처, `RateLimitFilter`의 rate-limit 키처럼 순수 wiring 목적에 한한다. `*Request`/`*Response`/`*Api`/`*Controller`처럼 실제 프레젠테이션 로직이 담긴 타입은 여전히 참조 대상이 아니다. URL 경로는 그 경로를 실제로 노출하는 `*Api`와 동기화돼야 하는 도메인 고유의 사실이므로, `global`이 별도 상수로 다시 소유하기보다 도메인 쪽 단일 소스를 참조하는 쪽이 응집도가 높다.

**(2026-09-20 결정, 이슈 #47)**: `*Endpoints`가 경로의 단일 소스이고, `*Api` 자신도 `@GetMapping`/`@PostMapping` 등에 리터럴 경로 문자열 대신 같은 도메인의 `*Endpoints` 상수를 그대로 참조한다(예: `AuthApi`의 `@PostMapping(AuthEndpoints.LOGIN)`, `MemberApi`의 `@GetMapping(MemberEndpoints.ME)`). `*Api`가 리터럴 문자열을 따로 갖고 `*Endpoints`가 그걸 베끼는 구조가 아니다. 클래스 레벨 `@RequestMapping`으로 베이스 경로를 나누지 않고, `*Endpoints`가 `BASE + 세부경로`로 이미 완성해 둔 전체 경로를 각 메서드의 매핑 애노테이션에 직접 넣는다.

## 계층별 역할

| 계층 | 역할 |
|---|---|
| `domain` | 순수 도메인 모델, 도메인 서비스, `*Repository`·`*Reader` 인터페이스. 비즈니스 로직이 실제로 사는 곳 |
| `application` | `*UseCase` 인터페이스 + `*Service` 구현체(`@CommandService`/`@QueryService`). 오케스트레이션만 담당 |
| `presentation` | `*Controller`, `*Api` 인터페이스, `*Request`/`*Response` DTO, 수동 실행 진입점 `*Runner`(`ApplicationRunner`) |
| `infrastructure` | `*RepositoryImpl`(`*Repository` 구현체), `*JpaEntity`(Domain↔JPA 변환용 `from()`/`toDomain()` 포함), Redis 등 외부 연동, 파일 원본을 읽는 `*Reader` 구현체 |

## 계층별 타입 어휘 (Layer Type Vocabulary)

각 계층은 자기 타입만 다루고, 계층 경계를 넘을 땐 반드시 명시적 변환을 거친다. Service가 `*Request`를 직접 받거나 도메인 엔티티·`*JpaEntity`를 그대로 반환하는 일은 없다.

| 계층 | 입력 | 출력 | 변환 지점 |
|---|---|---|---|
| Presentation | `*Request` | `*Response` | `Request.toCommand()`/`toQuery()`, `*Response.from(Result)` |
| Application (조회) | 단일 식별자면 원시값, 그 외엔 `*Query` | `*Result` | `*UseCase` 인터페이스 시그니처 |
| Application (쓰기) | `*Command` | `*Result` 또는 `void`(반환값이 불필요한 경우, 예: 삭제) | `*UseCase` 인터페이스 시그니처 |
| Domain | 원시값 | 도메인 객체 | 도메인 팩토리 메서드/생성자 |
| Infrastructure | 도메인 객체 | `*JpaEntity` | `*JpaEntity.from()`/`toDomain()` (재사용·비단순 매핑 시에만 별도 `*Mapper`) |

- `*Result`는 도메인 엔티티를 감싸지 않는다. 도메인 필드를 평탄화한 별도 DTO로 만들어 도메인 객체가 Application 계층 밖으로 새어나가지 않게 한다. **크로스 도메인으로 주고받을 때도 이 `*Result`가 그대로 계약 역할을 한다.** 예를 들어 `RefreshService`가 `member.GetMemberUseCase`를 호출해 받는 것도 `MemberResult`다.
- **DTO(`*Command`/`*Result`/`*Request`/`*Response`)는 자기 자신에게 정적 팩토리(`from`/`of`)나 변환 메서드(`to*`)를 두고 그 안에서 필드를 채운다.** Service/Controller 같은 호출부가 `new`로 필드를 나열해 직접 조립하지 않는다 (예: `MemberResult.from(Member)`, `TokenResult.of(...)`). 변환 대상이 다른 도메인 소속이면, 이미 그 도메인을 의존하고 있는 쪽이 변환 메서드를 소유해 크로스 도메인 의존 방향을 거스르지 않는다 (예: `auth.OAuthMemberResult.toMemberCreateCommand()`는 `auth → member` 기존 의존 방향과 같은 쪽에 둔 것이지, `member.MemberCreateCommand`에 `from(OAuthMemberResult)`를 두어 역방향 의존을 만들지 않는다).
- 조회(Query) 파라미터가 단일 식별자(ID, code 등 원시값 하나)면 감싸지 않고 그대로 받는다. 파라미터가 2개 이상이거나 필터·정렬·페이징처럼 확장 가능성이 있는 조건이면 `*Query`로 감싼다 (단일 식별자까지 감싸는 건 계층 응집도보다 보일러플레이트 비용이 더 크다).
- 반환값이 필요 없는 Command(삭제 등)는 `void`를 허용한다. 빈 `*Result`를 억지로 만들지 않는다.

## 크로스 도메인 참조 예시

- 크로스 도메인 조율이 필요한 `*Service`는 별도 Facade 없이, 대상 도메인의 `*UseCase` 인터페이스(및 `*Result`)를 직접 의존한다. Repository·구현체 직접 참조는 금지.
- 예: `auth/application/RefreshService`는 Member 역할 조회를 위해 `member.GetMemberUseCase`를 직접 의존한다.
- **가드레일**: 하나의 `*Service`가 크로스 도메인 `*UseCase`를 2개 이상 의존해야 하는 상황이 오면, Facade를 다시 두는 대신 그 UseCase 자체가 너무 커진 신호로 보고 쪼갤 수 있는지부터 검토한다.

## 크로스 도메인 부수효과: Controller 직접 호출 vs 도메인 이벤트

크로스 도메인 부수효과(예: 회원 삭제 시 로그아웃 처리)를 어디서 조율할지는 **부수효과 실패가 원래 트랜잭션을 되돌려야 하는지**로 판단한다.

- **Controller에서 각 도메인 `*UseCase`를 직접 순차 호출**: 부수효과가 실패했을 때 원본 작업도 함께 실패(롤백)해야 정합성이 유지되는 경우. 크로스 도메인 `*UseCase` 의존이 1개뿐이고 트리거·부수효과가 단순하면 이 방식으로 충분하며, 굳이 이벤트 인프라를 먼저 두지 않는다.
- **도메인 이벤트(`@TransactionalEventListener(phase = AFTER_COMMIT)`)**: 원본 트랜잭션이 커밋된 뒤에만 의미가 있고, 부수효과가 실패해도 원본 작업을 되돌릴 이유가 없는 후속 정리(cleanup) 성격일 때. 예를 들어 회원 삭제는 그 자체로 완결된 사실이며, 뒤이은 로그아웃 처리(리프레시 토큰 삭제·액세스 토큰 블랙리스트 등록)가 일시적으로 실패하더라도 이미 삭제된 회원을 되살릴 이유는 없다 — 그래서 `MemberDeletedEvent` 기반으로 분리한다.

**이벤트 컨벤션**:
- 네이밍: `<Entity><과거분사>Event` (예: `MemberDeletedEvent`). 순수 도메인 개념(ID 등)뿐 아니라 구독 측이 필요로 하는 요청 컨텍스트(accessToken, ip 등)도 담을 수 있다 — 이벤트는 "구독자에게 필요한 계약"이지, 발행 도메인의 순수 도메인 모델 그 자체는 아니다. 다만 그 컨텍스트가 여러 필드로 늘어나 이벤트의 성격이 모호해지면 그 시점에 페이로드 축소를 재검토한다.
- 정의 위치: 발행하는 도메인의 `application.event` 패키지 (`domain`이 아니다 — `domain`은 프레임워크는 물론, accessToken처럼 다른 도메인/기술 맥락에 속하는 개념도 알지 못해야 한다).
- 발행 위치: 트랜잭션 경계를 가진 `@CommandService` 구현체 내부에서 `ApplicationEventPublisher.publishEvent(...)`로 발행한다 (필드 주입). 트랜잭션 밖(Controller 등)에서 발행하면 `AFTER_COMMIT` 리스너가 걸리지 않는다.
- 구독 위치: 구독하는 도메인의 `application.listener` 패키지에 `@Component` 클래스를 두고 `@TransactionalEventListener(phase = AFTER_COMMIT)` 메서드로 구독한다. 리스너는 대상 도메인의 `*UseCase`를 직접 의존해 위임만 하고, 그 안에 비즈니스 로직을 직접 작성하지 않는다 (`@Component`는 기술적 wiring 전용이라는 기존 원칙과 동일).
- 공유 마커 인터페이스나 베이스 클래스(`common.event` 등)는 두지 않는다. Spring의 `ApplicationEventPublisher`/`@TransactionalEventListener`는 특정 타입을 요구하지 않고, 이벤트가 아직 소수라 공유 추상화를 선제적으로 둘 근거가 없다 (YAGNI). 이벤트 종류가 늘어 정말 공통 로직(로깅, 재시도 등)이 필요해지는 시점에 재검토한다.

## 트랜잭션 / CQRS

`@CommandService`(`@Service` + `@Transactional`), `@QueryService`(`@Service` + `@Transactional(readOnly = true)`) 메타 애노테이션을 `common.annotation`에 두고 모든 `*Service`에 부착한다.

- 메서드 단위 `@Transactional`은 개별 부착하지 않는다. 클래스 하나 = 트랜잭션 성격 하나가 원칙이다. 이게 지켜지지 않으면 그 자체로 UseCase가 여러 책임을 겸하고 있다는 신호다.
- `spring.jpa.open-in-view=false` (OSIV 비활성화). Controller에서 지연 로딩 사용 금지.

## 영속성 (PostgreSQL / pgvector)

**(2026-09-20 결정, 이슈 #53)**: DB는 MySQL 대신 PostgreSQL 17을 쓰고, 로컬 compose와 Testcontainers 모두 `pgvector/pgvector:pg17` 이미지를 쓴다. 두 환경이 같은 이미지를 써서 환경 간 차이를 없애고, pgvector 확장이 필요해질 때 이미지를 바꾸지 않아도 된다. 도메인과 데이터가 작을 때 전환하는 편이 member/auth가 커진 뒤보다 변경 범위가 작다. MySQL을 유지하는 안은 pgvector를 쓸 수 없어 유사도 검색을 도입할 때 다시 전환해야 하고, 그때는 데이터가 커져 있어 택하지 않았다. 확장이 없는 일반 `postgres` 이미지는 나중에 확장 설치를 따로 해야 해서 택하지 않았다. `pgvector/pgvector` 이미지는 이름으로 PostgreSQL을 인식하지 못해 `spring-boot-docker-compose`가 datasource를 연결하지 못한다(`bootRun`에서 `Failed to configure a DataSource`로 확인). 그래서 `compose.yaml`의 `postgres` 서비스에 `org.springframework.boot.service-connection=postgres` 라벨을 반드시 유지한다. 테스트는 `@ServiceConnection` 빈으로 연결하므로 이 문제를 잡지 못한다. 이미지를 바꾸면 `bootRun`으로 직접 확인한다.

**(2026-09-20 결정, 이슈 #53)**: `FlavorProfile`(int 5컬럼, #74에서 산미 추가)과 `MoodProfile`(int 4컬럼)은 지금처럼 int 컬럼 `*Embeddable`로 유지하고 vector 컬럼으로 바꾸지 않는다. 점수는 `Pairing.score()`가 `FlavorProfile.similarity()`와 `MoodProfile.fitness()`를 자바에서 계산해 합산하고, 데이터도 술 8개, 안주 6개뿐이라 ANN 인덱스(ivfflat/hnsw)를 쓸 지점도 이득도 없다. vector 컬럼으로 교체하는 안은 스키마와 매핑만 복잡해지고, int 컬럼과 vector 컬럼을 병행하는 안은 이중 관리가 되어 택하지 않았다. pgvector 0.8.4(`pg17` 태그 기준 확인 시점의 버전)에서 `vector(4)`의 `<->` 거리가 현재 공식과 `similarity = 1 - distance / 10`으로 정확히 대응하는 것을 확인했으므로(5축인 `FlavorProfile`은 `vector(5)`와 `1 - distance / sqrt(125)`로 대응한다) 나중에 전환해도 점수의 의미는 바뀌지 않는다. 재검토 조건은 유사한 술이나 안주를 DB 쿼리로 찾는 기능 이슈가 생기거나, 후보가 수천 건 이상으로 늘어 자바 전건 계산이 병목이 될 때다. 그때도 도메인 VO는 그대로 두고 `*Embeddable`과 `*JpaEntity`(infrastructure)에만 `@JdbcTypeCode(SqlTypes.VECTOR)`와 `@Array(length = 축 개수)`로 매핑한다.

**(2026-09-27 결정, 이슈 #74)**: `FlavorProfile`에 산미(`acidity`)를 5번째 유사도 축으로 추가했다. 기존 테이블에 컬럼을 더할 때는 마이그레이션 도구 없이 `*Embeddable` 필드에 `@ColumnDefault`를 붙여 `ddl-auto: update`가 `default ... not null` 컬럼을 만들게 한다. 기본값이 없으면 행이 있는 DB에서 `add column ... not null`이 실패하고, 이어서 `data.sql`의 INSERT까지 실패해 앱이 뜨지 않는다. 수동 `ALTER` 스크립트는 둘 위치가 없어 택하지 않았다. 스키마 관리는 이후 Flyway를 도입해 마이그레이션 스크립트로 전환한다. 이번 이슈에서 함께 도입하지 않은 이유는 이 이슈의 범위를 넘기 때문이다. `@ColumnDefault`는 Flyway 도입 전까지의 임시 대응이다. Flyway를 도입하면 enum `check` 제약 문제(#53)도 마이그레이션 스크립트로 함께 해결한다. 산미는 "비슷할수록 잘 맞는다"는 유사도 축으로만 반영했다. 산미가 기름진 안주를 잡아주는 대비 효과는 별도 규칙이 필요하므로 따로 이슈로 다룬다. 축이 늘어 정규화 분모가 100에서 125로 바뀌면서 시드 48쌍 중 `MATCH_THRESHOLD`(0.7) 이상인 쌍이 16쌍에서 12쌍으로 줄었다. 새로 매칭되는 조합(레드 와인과 골뱅이무침, 막걸리와 과일안주)이 페어링 상식에 맞아 임계값은 0.7로 유지했다. 축을 더 추가할 때도 시드 전체의 매칭 비율을 다시 계산해 임계값을 확인한다. `@ColumnDefault`는 #61에서 Flyway 도입과 함께 제거했다.

**(2026-09-20 결정, 이슈 #53)**: `CREATE EXTENSION IF NOT EXISTS vector`는 지금 실행하지 않는다. 쓰는 코드가 없으므로, vector 컬럼을 도입하는 이슈에서 마이그레이션 스크립트로 실행한다.

**(2026-09-20 결정, 이슈 #53)**: 인덱스와 제약은 마이그레이션 스크립트에 선언한다. PostgreSQL은 MySQL(InnoDB)과 달리 FK 컬럼을 자동으로 인덱싱하지 않기 때문이다. 조회 조건 컬럼의 인덱스도 어느 DB에서든 Hibernate가 만들어 주지 않는다. `anju_allergy_type.anju_id` FK는 복합 PK의 선두 컬럼이라 별도 인덱스가 필요 없다. `pairing_feedback`의 FK 컬럼(#61)을 인덱싱하지 않은 근거는 #61 결정 참고. `member`는 로그인마다 조회하는 `(social_provider, social_id)`에 unique 제약(`uk_member_social_login`)을 선언해 조회 성능과 중복 가입 방지를 함께 해결했다. 동시 첫 로그인 경쟁에서 두 번째 저장이 던지는 `DataIntegrityViolationException`은 `MemberRepositoryImpl.save`가 제약 이름(`uk_member_social_login`)으로 구분해 `MemberAlreadyExistsException`(409)으로 변환한다(#63). 영속성 기술 예외와 제약 이름은 infrastructure만 알아야 하므로 application이 아니라 `*RepositoryImpl`에서 변환하고, 다른 무결성 위반은 변환하지 않고 그대로 전파한다. FK 제약이 생기거나 새 조회 조건 컬럼이 생길 때마다 인덱스가 필요한지 함께 검토한다.

**(2026-09-20 결정, 이슈 #53)**: `@Enumerated(STRING)` 컬럼은 `varchar(255)`와 허용 값 목록 `check` 제약으로 생성되는데, `ddl-auto: update`는 이미 만들어진 `check` 제약을 갱신하지 않는다. 그래서 enum 값을 추가하면 이미 스키마가 만들어진 DB에서는 새 값 INSERT가 제약 위반으로 실패한다. dev/prod는 아직 빈 DB라 지금은 영향이 없지만, enum 값을 추가하는 이슈에서는 수동 `ALTER`나 마이그레이션 도구 도입을 함께 결정해야 한다. MySQL의 native `enum` 컬럼도 같은 한계가 있었으므로 이번 전환으로 생긴 회귀는 아니다. #61 이후 enum 값 추가는 새 마이그레이션에서 `check` 제약을 수정한다.

**(2026-09-27 결정, 이슈 #61)**: 스키마와 시드 데이터는 Flyway 마이그레이션(`db/migration/V{n}__설명.sql`)으로 관리하고, `ddl-auto`는 `validate`로 둬서 엔티티와 스키마 불일치를 기동 단계에서 잡는다. `data.sql`은 기동할 때마다 시드를 지우고 다시 넣어서 IDENTITY id가 재기동마다 바뀌었다. 그 결과 FK 없이 id만 저장하던 `pairing_feedback`이 없는 행을 가리키게 되어 폐기했다. 이미 적용된 V 파일은 수정하지 않는다. 스키마 변경, enum 값 추가(`check` 제약 수정), 시드 추가는 모두 새 버전 파일로 한다. 시드는 id를 명시해 넣고, 파일 끝에서 `setval(pg_get_serial_sequence(...), MAX(id))`로 시퀀스를 보정한다. 보정하지 않으면 다음 INSERT가 시드 id와 충돌한다. 테이블 이름은 `@Table(name)`으로 지정해 `_jpa_entity` 접미사가 스키마에 드러나지 않게 했다. dev/prod는 한 번도 기동된 적 없어 스키마가 비어 있으므로 baseline 없이 V1부터 적용했다. Flyway는 데이터 유무가 아니라 스키마가 비어 있는지로 판단하므로, 옛 `*_jpa_entity` 테이블이 남은 DB(로컬 compose 등)는 볼륨을 초기화한 뒤 기동한다. `drink.name`은 X-Wines 데이터(#77)에 같은 이름의 와인이 204건 있어서 유니크 제약을 걸지 않았다. `pairing_feedback`의 `member_id`는 도메인 간 참조라 FK를 걸지 않았다. drink, anju, music mood FK 컬럼은 조회 조건이 없고 카탈로그를 삭제하는 일도 드물어 인덱스를 두지 않았다. 테스트 DB에도 V2 시드가 들어가므로, 카탈로그 내용에 기대는 테스트는 `@Sql("/sql/clear-pairing-catalog.sql")`로 카탈로그를 비우고 시작한다.

**(2026-09-27 결정, 이슈 #75)**: 음료 데이터는 소스마다 단위가 달라서(와인은 제품, 맥주는 스타일) `Drink`를 제품 단위로 통일하고, 분류(`DrinkCategory`)와 기본 맛은 `DrinkStyle`이 갖는다. `Drink`는 `DrinkStyle`을 객체가 아니라 `drinkStyleId`로 참조한다. 스타일은 적재 기준 데이터이고 제품은 소스별로 늘어나서 생명주기가 다르므로 별도 애그리거트로 뒀다. 추천 점수는 지금처럼 `Drink`의 `flavorProfile`로 계산하고, 스타일의 기본 맛은 적재할 때 제품 맛의 초기값으로만 쓴다. `Drink`는 `(source, external_id)` 유니크 제약으로 식별한다. `name`은 동명 와인이 있어 식별자로 쓸 수 없기 때문이다(#61). PostgreSQL 유니크 제약은 NULL끼리 중복을 허용하므로 `external_id`는 `NOT NULL`로 두고, 시드 음료에는 `source = 'SEED'`와 `'soju'` 같은 읽을 수 있는 키를 넣었다. `DrinkSource`에는 지금 쓰는 `SEED`만 두고, `X_WINES` 등은 적재 이슈(#76, #77)의 마이그레이션에서 `check` 제약과 함께 추가한다. BJCP는 스타일의 출처라 `Drink`의 source 값이 아니다. 값이 늘어날 enum의 `check` 제약에는 `ck_<테이블>_<컬럼>` 이름을 붙여 후속 마이그레이션이 이름으로 DROP한 뒤 다시 ADD할 수 있게 했다. `DrinkStyle`에는 `name` 유니크 제약만 걸었다. BJCP 스타일 적재(#76)에서 출처 식별이 필요해지면 그때 `(source, external_id)`를 추가한다. `drink.drink_style_id` FK는 이 컬럼으로 조회하는 곳이 없어 인덱스를 두지 않았다. 스타일별 음료 조회가 생기면 인덱스를 추가한다.

**(2026-09-27 결정, 이슈 #76)**: 음료 데이터 적재는 셸 스크립트가 아니라 Spring Boot 적재 러너로 한다. `./gradlew importDrinkStyles`가 `import.drink-style.enabled=true`를 넘기면 `presentation/runner`의 `ImportDrinkStyleRunner`가 `ImportDrinkStyleUseCase`를 호출한다. 이 속성이 없는 일반 기동과 배포에서는 `@ConditionalOnProperty` 때문에 러너 빈이 만들어지지 않는다. 적재 전용 프로필은 택하지 않았다. local, dev, prod 3개 프로필 체계에 개념이 하나 늘어나기 때문이다. 맛 계산 규칙은 도메인의 `FlavorRule`에 둔다. `ImportDrinkStyleService` 단위 테스트가 실제 `FlavorRule`로 계산 결과를 검증한다. testing 스킬의 계층별 테스트 범위에 도메인 계층이 없으므로, IBU 경계값이나 mouthfeel 키워드 우선순위를 따로 검증하는 도메인 테스트는 두지 않는다. 처음에는 bash와 psql로 staging 테이블에 넣고 SQL로 맛을 계산하는 방식을 구현했다가 폐기했다. 그 방식은 규칙 계산을 테스트할 수 없었고, 셸 heredoc 안의 SQL이 #77, #78을 거치며 계속 길어질 구조였다. 파일 읽기는 외부 연동이므로 `domain/reader`의 인터페이스를 `infrastructure/reader`의 `JsonBjcpStyleReader`와 `CsvFlavorRuleReader`가 구현한다. 규칙표 CSV는 버전 관리되는 설정이라 `src/main/resources/import/rules/`에 둔다. BJCP 원본 JSON은 저작권이 BJCP에 있어 커밋하지 않는다. 대신 `scripts/import/fetch-bjcp.sh`가 beerjson/bjcp-json의 고정 커밋에서 받아 체크섬으로 검증한다. Spring Batch도 택하지 않았다. 적재 규모가 최대 천 건 정도라 청크 처리와 재시작이 필요 없고, 메타데이터 테이블과 기동 시 자동 실행을 막는 설정만 늘어나기 때문이다. X-Wines Full(약 10만 건)이나 평점 데이터를 적재하거나 정기 동기화가 필요해지면 Spring Batch를 재검토한다. #75에서 미뤄 둔 `drink_style`의 `(source, external_id)`는 이번에 추가했다. `DrinkStyleSource`는 `SEED`와 `BJCP`이고, 국내 유통 맥주 매핑용으로 `DrinkSource`에 `CURATED`를 추가했다. 레포에 두는 단독 `.sql` 파일은 Flyway 마이그레이션(`db/migration`)과 테스트 픽스처(`src/test/resources/sql`)뿐이다. 태스크와 러너, 속성 이름은 2026-10-07 결정에서 `importDrinks`, `ImportDrinkRunner`, `import.drink.enabled`로 바꿨다.

**(2026-10-07 결정, 이슈 #76)**: 국내 유통 맥주는 사람이 고른 매핑표(`src/main/resources/import/curated/beers.csv`)로 `Drink`에 적재한다. 매핑표는 제품마다 BJCP 스타일 id를 하나 가리키고, `ImportCuratedDrinkService`가 `(BJCP, 스타일 id)`로 `DrinkStyle`을 찾아 그 기본 맛을 제품 맛으로 복사한 뒤 `(CURATED, external_id)`로 upsert한다. 제품별 맛 보정 열은 두지 않았다. 사람이 손으로 정한 값이 들어가면 맛을 규칙표로만 계산한다는 원칙이 깨지기 때문이다. 그 결과 같은 스타일(특히 2A International Pale Lager)에 묶인 제품은 맛이 같아진다. 추천 결과에 같은 맛의 제품이 몰리는 문제는 후속 이슈 #87에서 다룬다. 매핑한 BJCP 스타일이 없으면(아직 적재되지 않았거나 맛을 정할 수 없어 스타일 적재에서 제외된 경우) 제품을 저장하지 않고 적재 리포트에 남긴다. 제품이 스타일을 참조하므로 러너는 스타일을 먼저 적재한다. 유스케이스마다 트랜잭션이 따로라 스타일 적재가 먼저 커밋된다. 적재 대상이 스타일에서 제품까지 늘어나 Gradle 태스크와 러너, 속성 이름을 `importDrinks`, `ImportDrinkRunner`, `import.drink.enabled`로 바꿨다. #77의 X-Wines 적재도 같은 진입점에 붙인다. 시드의 `맥주` 행은 실제 제품이 아니라서 V5 마이그레이션에서 카스 프레시(`CURATED`, `cass-fresh`)로 `UPDATE`했다. `pairing_feedback`이 FK로 참조하므로 행을 지우지 않고 id를 유지했다. 마이그레이션 시점에는 BJCP 스타일이 없어서 스타일 참조와 맛은 시드 라거 그대로 두고, 이후 적재가 같은 `(source, external_id)`로 이 행을 갱신한다. 시드 스타일 `라거`는 남겨 두었다. V5 직후 적재 전까지는 카스 프레시가 이 스타일을 참조하므로 마이그레이션에서 지울 수 없고, 적재 후 참조가 사라진 스타일을 정리하는 일은 이번 이슈 범위 밖이기 때문이다.

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

## 응답 포맷

`StandardResponse<T>`(`common.response`)가 모든 응답을 감싼다. 필드는 `boolean ok`, `int status`(HTTP 상태 코드), `T data`로 확정되어 있다. 성공 응답은 `StandardResponse.success(status, data)`(반환값 없는 성공은 `data` 없는 오버로드), 에러 응답은 `StandardResponse.fail(status, ErrorResponse)` 정적 팩토리로 생성한다.

## 정적분석 도구 미도입 사유

Konsist/ArchUnit 같은 아키텍처 자동검증 도구는 도입하지 않기로 했다. 단일 모듈 구조라 패키지 규칙 위반을 코드리뷰(`code-reviewer` 에이전트의 체크리스트 기반 수동 검토)로 잡는 게 더 실용적이라고 판단했기 때문이다. 계층 의존 방향처럼 판단이 섞이는 규칙은 이 방식으로 검증하고, EOF 개행·코드 스타일처럼 완전히 결정론적인 것만 스크립트/포맷터로 강제한다.
