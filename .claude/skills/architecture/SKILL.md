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
│   ├── domain/            # 순수 도메인 모델(Drink, DrinkStyle, Anju, MusicMood, Pairing), FlavorProfile/MoodProfile/Occasion 값 객체, PairingFeedback, *Repository 인터페이스, 규칙표로 맛을 계산하는 FlavorRule/WineFlavorRule/AnjuRule, 추천 후보를 계산하는 DrinkAnjuScores/CandidateThresholds, 적재 원본을 읽는 *Reader 인터페이스(reader)
│   ├── application/       # *UseCase 인터페이스 + *Service 구현체 (예: PairingQueryService, PairingCommandService, Query/Command 레벨. 파일을 읽는 적재는 UseCase 단위로 분리: ImportDrinkStyleService, ImportCuratedDrinkService, ImportXWineService, ImportAnjuService)
│   ├── presentation/      # Controller, *Api, 수동 적재 진입점 ImportCatalogRunner(runner)
│   └── infrastructure/    # DrinkRepository/AnjuRepository/PairingFeedbackRepository 구현체, JPA 엔티티, *Reader 구현체(reader: JsonBjcpStyleReader, CsvFlavorRuleReader, CsvCuratedDrinkReader, CsvXWineReader, CsvWineFlavorRuleReader, CsvBeerHarmonizeRuleReader, CsvCuratedAnjuReader, CsvAnjuNutritionReader, CsvAnjuRuleReader)
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

`global`은 같은 이유로 도메인의 `presentation` 계층에 있는 `*Endpoints`(비즈니스 로직·프레임워크 의존 없이 URL 경로 문자열만 담은 상수 클래스, 예: `auth.presentation.AuthEndpoints`)도 참조할 수 있다. 이는 `SecurityConfig`의 permitAll 매처처럼 순수 wiring 목적에 한한다. `*Request`/`*Response`/`*Api`/`*Controller`처럼 실제 프레젠테이션 로직이 담긴 타입은 여전히 참조 대상이 아니다. URL 경로는 그 경로를 실제로 노출하는 `*Api`와 동기화돼야 하는 도메인 고유의 사실이므로, `global`이 별도 상수로 다시 소유하기보다 도메인 쪽 단일 소스를 참조하는 쪽이 응집도가 높다.

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

**(2026-09-20 결정, 이슈 #53)**: `FlavorProfile`(int 5컬럼, #74에서 산미 추가)과 `MoodProfile`(int 4컬럼)은 지금처럼 int 컬럼 `*Embeddable`로 유지하고 vector 컬럼으로 바꾸지 않는다. 점수는 자바에서 계산한다. 음료×안주 점수(`Pairing.drinkAnjuScore`)는 `FlavorProfile.similarity()`에 가중치와 감점을 적용한 값이고, 음악 점수(`Pairing.scoreMusicMoods`)는 `MoodProfile.fitness()`에 가중치를 곱한 값이다. 조합 점수는 두 점수의 합이다(#79). vector 컬럼으로 교체하는 안은 스키마와 매핑만 복잡해지고, int 컬럼과 vector 컬럼을 병행하는 안은 이중 관리가 되어 택하지 않았다. pgvector 0.8.4(`pg17` 태그 기준 확인 시점의 버전)에서 `vector(4)`의 `<->` 거리가 현재 공식과 `similarity = 1 - distance / 10`으로 정확히 대응하는 것을 확인했으므로(5축인 `FlavorProfile`은 `vector(5)`와 `1 - distance / sqrt(125)`로 대응한다) 나중에 전환해도 점수의 의미는 바뀌지 않는다. **(2026-10-17 재확인, 이슈 #101)**: `importDrinks` 적재 후 음료는 1,026종(시드 8종, 새로 적재된 국내 유통 맥주 20종, X-Wines 와인 998종)이고 안주는 6개, 음악은 8개다. 시드 8종 중 2행은 V5, V6에서 카스 프레시와 Louis Jadot로 대체됐다. 조합은 약 4.9만 개다. 처음 근거였던 데이터 규모(술 8개, 안주 6개)는 더 이상 맞지 않지만, ANN 인덱스(ivfflat/hnsw) 미도입은 유지한다. 처음 재검토 조건이던 "후보가 수천 건 이상으로 늘어 자바 전건 계산이 병목이 될 때"는 #79에서 연산량을 줄여 해결했다. ANN 인덱스는 쿼리 벡터 하나에 가까운 행을 찾는 용도다. 추천은 맛 유사도에서 감점을 빼고 음악 점수를 더한 합산 점수로 순위를 매기고, 스타일별 최고와 무알코올 옵션까지 골라야 한다. 그래서 거리 순 조회로 대신할 수 없다. 이 규모에서 #79 방식의 자바 계산은 p95 103ms(#79 측정)로 병목이 아니다. 재검토 조건은 유사한 술이나 안주를 DB 쿼리로 찾는 기능 이슈가 생기거나, 음료가 X-Wines Full(약 10만 건)처럼 수만 건 이상으로 늘어 #79 방식으로도 자바 계산이 병목이 될 때다. 그때도 도메인 VO는 그대로 두고 `*Embeddable`과 `*JpaEntity`(infrastructure)에만 `@JdbcTypeCode(SqlTypes.VECTOR)`와 `@Array(length = 축 개수)`로 매핑한다.

**(2026-09-27 결정, 이슈 #74)**: `FlavorProfile`에 산미(`acidity`)를 5번째 유사도 축으로 추가했다. 기존 테이블에 컬럼을 더할 때는 마이그레이션 도구 없이 `*Embeddable` 필드에 `@ColumnDefault`를 붙여 `ddl-auto: update`가 `default ... not null` 컬럼을 만들게 한다. 기본값이 없으면 행이 있는 DB에서 `add column ... not null`이 실패하고, 이어서 `data.sql`의 INSERT까지 실패해 앱이 뜨지 않는다. 수동 `ALTER` 스크립트는 둘 위치가 없어 택하지 않았다. 스키마 관리는 이후 Flyway를 도입해 마이그레이션 스크립트로 전환한다. 이번 이슈에서 함께 도입하지 않은 이유는 이 이슈의 범위를 넘기 때문이다. `@ColumnDefault`는 Flyway 도입 전까지의 임시 대응이다. Flyway를 도입하면 enum `check` 제약 문제(#53)도 마이그레이션 스크립트로 함께 해결한다. 산미는 "비슷할수록 잘 맞는다"는 유사도 축으로만 반영했다. 산미가 기름진 안주를 잡아주는 대비 효과는 별도 규칙이 필요하므로 따로 이슈로 다룬다. 축이 늘어 정규화 분모가 100에서 125로 바뀌면서 시드 48쌍 중 `MATCH_THRESHOLD`(0.7) 이상인 쌍이 16쌍에서 12쌍으로 줄었다. 새로 매칭되는 조합(레드 와인과 골뱅이무침, 막걸리와 과일안주)이 페어링 상식에 맞아 임계값은 0.7로 유지했다. 축을 더 추가할 때도 시드 전체의 매칭 비율을 다시 계산해 임계값을 확인한다. `@ColumnDefault`는 #61에서 Flyway 도입과 함께 제거했다.

**(2026-09-20 결정, 이슈 #53)**: `CREATE EXTENSION IF NOT EXISTS vector`는 지금 실행하지 않는다. 쓰는 코드가 없으므로, vector 컬럼을 도입하는 이슈에서 마이그레이션 스크립트로 실행한다.

**(2026-09-20 결정, 이슈 #53)**: 인덱스와 제약은 마이그레이션 스크립트에 선언한다. PostgreSQL은 MySQL(InnoDB)과 달리 FK 컬럼을 자동으로 인덱싱하지 않기 때문이다. 조회 조건 컬럼의 인덱스도 어느 DB에서든 Hibernate가 만들어 주지 않는다. `anju_allergy_type.anju_id` FK는 복합 PK의 선두 컬럼이라 별도 인덱스가 필요 없다. `pairing_feedback`의 FK 컬럼(#61)을 인덱싱하지 않은 근거는 #61 결정 참고. `member`는 로그인마다 조회하는 `(social_provider, social_id)`에 unique 제약(`uk_member_social_login`)을 선언해 조회 성능과 중복 가입 방지를 함께 해결했다. 동시 첫 로그인 경쟁에서 두 번째 저장이 던지는 `DataIntegrityViolationException`은 `MemberRepositoryImpl.save`가 제약 이름(`uk_member_social_login`)으로 구분해 `MemberAlreadyExistsException`(409)으로 변환한다(#63). 영속성 기술 예외와 제약 이름은 infrastructure만 알아야 하므로 application이 아니라 `*RepositoryImpl`에서 변환하고, 다른 무결성 위반은 변환하지 않고 그대로 전파한다. FK 제약이 생기거나 새 조회 조건 컬럼이 생길 때마다 인덱스가 필요한지 함께 검토한다.

**(2026-09-20 결정, 이슈 #53)**: `@Enumerated(STRING)` 컬럼은 `varchar(255)`와 허용 값 목록 `check` 제약으로 생성되는데, `ddl-auto: update`는 이미 만들어진 `check` 제약을 갱신하지 않는다. 그래서 enum 값을 추가하면 이미 스키마가 만들어진 DB에서는 새 값 INSERT가 제약 위반으로 실패한다. dev/prod는 아직 빈 DB라 지금은 영향이 없지만, enum 값을 추가하는 이슈에서는 수동 `ALTER`나 마이그레이션 도구 도입을 함께 결정해야 한다. MySQL의 native `enum` 컬럼도 같은 한계가 있었으므로 이번 전환으로 생긴 회귀는 아니다. #61 이후 enum 값 추가는 새 마이그레이션에서 `check` 제약을 수정한다.

**(2026-09-27 결정, 이슈 #61)**: 스키마와 시드 데이터는 Flyway 마이그레이션(`db/migration/V{n}__설명.sql`)으로 관리하고, `ddl-auto`는 `validate`로 둬서 엔티티와 스키마 불일치를 기동 단계에서 잡는다. `data.sql`은 기동할 때마다 시드를 지우고 다시 넣어서 IDENTITY id가 재기동마다 바뀌었다. 그 결과 FK 없이 id만 저장하던 `pairing_feedback`이 없는 행을 가리키게 되어 폐기했다. 이미 적용된 V 파일은 수정하지 않는다. 스키마 변경, enum 값 추가(`check` 제약 수정), 시드 추가는 모두 새 버전 파일로 한다. 시드는 id를 명시해 넣고, 파일 끝에서 `setval(pg_get_serial_sequence(...), MAX(id))`로 시퀀스를 보정한다. 보정하지 않으면 다음 INSERT가 시드 id와 충돌한다. 테이블 이름은 `@Table(name)`으로 지정해 `_jpa_entity` 접미사가 스키마에 드러나지 않게 했다. dev/prod는 한 번도 기동된 적 없어 스키마가 비어 있으므로 baseline 없이 V1부터 적용했다. Flyway는 데이터 유무가 아니라 스키마가 비어 있는지로 판단하므로, 옛 `*_jpa_entity` 테이블이 남은 DB(로컬 compose 등)는 볼륨을 초기화한 뒤 기동한다. `drink.name`은 X-Wines 데이터(#77)에 같은 이름의 와인이 204건 있어서 유니크 제약을 걸지 않았다. `pairing_feedback`의 `member_id`는 도메인 간 참조라 FK를 걸지 않았다. drink, anju, music mood FK 컬럼은 조회 조건이 없고 카탈로그를 삭제하는 일도 드물어 인덱스를 두지 않았다. 테스트 DB에도 V2 시드가 들어가므로, 카탈로그 내용에 기대는 테스트는 `@Sql("/sql/clear-pairing-catalog.sql")`로 카탈로그를 비우고 시작한다.

**(2026-09-27 결정, 이슈 #75)**: 음료 데이터는 소스마다 단위가 달라서(와인은 제품, 맥주는 스타일) `Drink`를 제품 단위로 통일하고, 분류(`DrinkCategory`)와 기본 맛은 `DrinkStyle`이 갖는다. `Drink`는 `DrinkStyle`을 객체가 아니라 `drinkStyleId`로 참조한다. 스타일은 적재 기준 데이터이고 제품은 소스별로 늘어나서 생명주기가 다르므로 별도 애그리거트로 뒀다. 추천 점수는 지금처럼 `Drink`의 `flavorProfile`로 계산하고, 스타일의 기본 맛은 적재할 때 제품 맛의 초기값으로만 쓴다. `Drink`는 `(source, external_id)` 유니크 제약으로 식별한다. `name`은 동명 와인이 있어 식별자로 쓸 수 없기 때문이다(#61). PostgreSQL 유니크 제약은 NULL끼리 중복을 허용하므로 `external_id`는 `NOT NULL`로 두고, 시드 음료에는 `source = 'SEED'`와 `'soju'` 같은 읽을 수 있는 키를 넣었다. `DrinkSource`에는 지금 쓰는 `SEED`만 두고, `X_WINES` 등은 적재 이슈(#76, #77)의 마이그레이션에서 `check` 제약과 함께 추가한다. BJCP는 스타일의 출처라 `Drink`의 source 값이 아니다. 값이 늘어날 enum의 `check` 제약에는 `ck_<테이블>_<컬럼>` 이름을 붙여 후속 마이그레이션이 이름으로 DROP한 뒤 다시 ADD할 수 있게 했다. `DrinkStyle`에는 `name` 유니크 제약만 걸었다. BJCP 스타일 적재(#76)에서 출처 식별이 필요해지면 그때 `(source, external_id)`를 추가한다. `drink.drink_style_id` FK는 이 컬럼으로 조회하는 곳이 없어 인덱스를 두지 않았다. 스타일별 음료 조회가 생기면 인덱스를 추가한다.

**(2026-09-27 결정, 이슈 #76)**: 음료 데이터 적재는 셸 스크립트가 아니라 Spring Boot 적재 러너로 한다. `./gradlew importDrinkStyles`가 `import.drink-style.enabled=true`를 넘기면 `presentation/runner`의 `ImportDrinkStyleRunner`가 `ImportDrinkStyleUseCase`를 호출한다. 이 속성이 없는 일반 기동과 배포에서는 `@ConditionalOnProperty` 때문에 러너 빈이 만들어지지 않는다. 적재 전용 프로필은 택하지 않았다. local, dev, prod 3개 프로필 체계에 개념이 하나 늘어나기 때문이다. 맛 계산 규칙은 도메인의 `FlavorRule`에 둔다. `ImportDrinkStyleService` 단위 테스트가 실제 `FlavorRule`로 계산 결과를 검증한다. testing 스킬의 계층별 테스트 범위에 도메인 계층이 없으므로, IBU 경계값이나 mouthfeel 키워드 우선순위를 따로 검증하는 도메인 테스트는 두지 않는다. 처음에는 bash와 psql로 staging 테이블에 넣고 SQL로 맛을 계산하는 방식을 구현했다가 폐기했다. 그 방식은 규칙 계산을 테스트할 수 없었고, 셸 heredoc 안의 SQL이 #77, #78을 거치며 계속 길어질 구조였다. 파일 읽기는 외부 연동이므로 `domain/reader`의 인터페이스를 `infrastructure/reader`의 `JsonBjcpStyleReader`와 `CsvFlavorRuleReader`가 구현한다. 규칙표 CSV는 버전 관리되는 설정이라 `src/main/resources/import/rules/`에 둔다. BJCP 원본 JSON은 저작권이 BJCP에 있어 커밋하지 않는다. 대신 `scripts/import/fetch-bjcp.sh`가 beerjson/bjcp-json의 고정 커밋에서 받아 체크섬으로 검증한다. Spring Batch도 택하지 않았다. 적재 규모가 최대 천 건 정도라 청크 처리와 재시작이 필요 없고, 메타데이터 테이블과 기동 시 자동 실행을 막는 설정만 늘어나기 때문이다. X-Wines Full(약 10만 건)이나 평점 데이터를 적재하거나 정기 동기화가 필요해지면 Spring Batch를 재검토한다. #75에서 미뤄 둔 `drink_style`의 `(source, external_id)`는 이번에 추가했다. `DrinkStyleSource`는 `SEED`와 `BJCP`이고, 국내 유통 맥주 매핑용으로 `DrinkSource`에 `CURATED`를 추가했다. 레포에 두는 단독 `.sql` 파일은 Flyway 마이그레이션(`db/migration`)과 테스트 픽스처(`src/test/resources/sql`)뿐이다. 태스크와 러너, 속성 이름은 2026-10-07 결정에서 `importDrinks`, `ImportDrinkRunner`, `import.drink.enabled`로 바꿨다.

**(2026-10-07 결정, 이슈 #76)**: 국내 유통 맥주는 사람이 고른 매핑표(`src/main/resources/import/curated/beers.csv`)로 `Drink`에 적재한다. 매핑표는 제품마다 BJCP 스타일 id를 하나 가리키고, `ImportCuratedDrinkService`가 `(BJCP, 스타일 id)`로 `DrinkStyle`을 찾아 그 기본 맛을 제품 맛으로 복사한 뒤 `(CURATED, external_id)`로 upsert한다. 제품별 맛 보정 열은 두지 않았다. 사람이 손으로 정한 값이 들어가면 맛을 규칙표로만 계산한다는 원칙이 깨지기 때문이다. 그 결과 같은 스타일(특히 2A International Pale Lager)에 묶인 제품은 맛이 같아진다. 추천 결과에 같은 맛의 제품이 몰리는 문제는 후속 이슈 #89에서 다룬다. 매핑한 BJCP 스타일이 없으면(아직 적재되지 않았거나 맛을 정할 수 없어 스타일 적재에서 제외된 경우) 제품을 저장하지 않고 적재 리포트에 남긴다. 제품이 스타일을 참조하므로 러너는 스타일을 먼저 적재한다. 유스케이스마다 트랜잭션이 따로라 스타일 적재가 먼저 커밋된다. 적재 대상이 스타일에서 제품까지 늘어나 Gradle 태스크와 러너, 속성 이름을 `importDrinks`, `ImportDrinkRunner`, `import.drink.enabled`로 바꿨다. #77의 X-Wines 적재도 같은 진입점에 붙인다. 시드의 `맥주` 행은 실제 제품이 아니라서 V5 마이그레이션에서 카스 프레시(`CURATED`, `cass-fresh`)로 `UPDATE`했다. `pairing_feedback`이 FK로 참조하므로 행을 지우지 않고 id를 유지했다. 마이그레이션 시점에는 BJCP 스타일이 없어서 스타일 참조와 맛은 시드 라거 그대로 두고, 이후 적재가 같은 `(source, external_id)`로 이 행을 갱신한다. 시드 스타일 `라거`는 남겨 두었다. V5 직후 적재 전까지는 카스 프레시가 이 스타일을 참조하므로 마이그레이션에서 지울 수 없고, 적재 후 참조가 사라진 스타일을 정리하는 일은 이번 이슈 범위 밖이기 때문이다.

**(2026-10-13 결정, 이슈 #77)**: X-Wines 와인(Slim, 1,007종)은 #76의 적재 러너(`importDrinks`)에 붙인다. 이슈 초안의 staging 테이블과 셸 스크립트는 #76에서 폐기한 방식이라 택하지 않았다. X-Wines는 CC0라서 BJCP와 달리 원본 CSV를 `scripts/import/data/x-wines/`에 커밋하고 `import.x-wines.path`로 읽는다. 원본이 Google Drive에만 있어서 fetch 스크립트를 두면 링크가 바뀔 때 재현할 수 없기 때문이다. 와인 스타일은 X-Wines의 `Type` 6종이고 `(X_WINES, type 슬러그)`로 식별한다. 스타일 이름은 시드 `레드 와인`과 유니크 제약이 겹치지 않도록 BJCP처럼 영문(`Red Wine` 등)으로 둔다. 스타일이 와인 데이터에서만 나오므로 맥주처럼 유스케이스를 나누지 않고, `ImportXWineService`가 한 트랜잭션에서 스타일을 먼저 적재한다. 제품 맛은 맥주처럼 스타일 맛을 그대로 복사하지 않는다. `Type`이 정한 스타일 기본 맛에서 무게감은 제품의 `Body`로, 산미는 `Acidity`로 덮어쓴다. X-Wines는 제품마다 이 값을 주는데, 버리면 정보를 잃고 같은 맛 쏠림(#89)도 커지기 때문이다. 규칙은 도메인 `WineFlavorRule`에, 값은 `wine-type.csv`, `wine-body.csv`, `wine-acidity.csv`에 둔다. Type, Body, Acidity는 닫힌 집합이라 enum도 검토했지만, 맛 수치를 `import/rules/`의 규칙표에서만 관리한다는 #76 원칙과 맥주와 와인이 공유하는 축 척도를 한곳에서 보기 위해 record와 CSV 규칙표를 택했다. Type별로 다른 계산 로직이 필요해지거나 데이터셋 버전이 바뀌어 값 집합이 달라지면 enum을 재검토한다. 도수 25% 초과 행은 적재하지 않는다. Slim 원본에서 이 조건에 걸리는 8건은 그라파와 마르 같은 증류주(38~50%)가 `Dessert`로 분류된 행이고, 와인의 최대 도수는 21%다. 규칙표에 없는 `Type`, `Body`, `Acidity` 값이 있는 와인도 기본값으로 채우지 않고 제외해 리포트에 남긴다. 클래스패스 전용이던 `ClasspathCsv`는 파일 시스템 원본도 읽도록 `Resource`를 받는 `HeaderCsv`로 바꿨다. 시드 `레드 와인`은 V6 마이그레이션에서 Louis Jadot Beaujolais(`X_WINES`, `111692`)로 `UPDATE`해 id를 유지한다. 근거는 V5와 같다. 시드와 무게감(3), 산미(4)가 같은 Red, Medium-bodied, High Acidity 와인 70종 중 국내에서 구하기 쉽고 시드 도수(13.0%)에 가장 가까운 제품을 골랐다.

**(2026-10-13 결정, 이슈 #77)**: 적재 서비스 3개(`ImportDrinkStyleService`, `ImportCuratedDrinkService`, `ImportXWineService`)에 반복되던 재적재 코드를 정리했다. 다시 적재할 때 덮어쓸 필드는 엔티티의 `overwriteWith(imported)`가 정한다. id와 식별 키 `(source, external_id)`를 지키는 규칙이 엔티티 안에 있어야 하기 때문이다. 자연키로 찾아서 있으면 덮어쓰고 없으면 새로 넣는 흐름은 Service에 그대로 둔다. Repository에 `saveBySourceAndExternalId`를 두는 안은 적재 정책이 infrastructure에 숨고 검증이 Testcontainers로 옮겨 가서 택하지 않았다. 람다를 조합하는 제네릭 헬퍼와 추상 베이스 Service도 각각 가독성과 UseCase 간 결합 때문에 택하지 않았다. 남은 4줄짜리 흐름은 지식이 아니라 오케스트레이션이라 중복으로 보지 않는다. 네 번째 소스(#78)에서 재적재 정책이 소스마다 갈라지기 시작하면 재검토한다.

**(2026-10-18 결정, 이슈 #107)**: 적재 서비스는 기존 행을 행마다 `(source, external_id)`로 조회하지 않는다. 출처별로 `findAllBySource` 한 번(안주는 SEED와 K_FIND가 섞여 있어 fetch join `findAll` 한 번)으로 불러와 Map에서 찾는다. 행마다 조회하면 안주는 알레르기 lazy 로딩까지 더해져 행 수의 두 배만큼 SELECT가 나간다. 같은 트랜잭션에서 불러온 엔티티는 영속성 컨텍스트에 있어 저장할 때 SELECT를 다시 실행하지 않는다. 적재 대상이 수만 건 이상으로 늘어 한 번에 불러오기 부담스러워지면 external_id 목록으로 나눠 조회하는 방식을 재검토한다.

**(2026-10-17 결정, 이슈 #96)**: 설정은 `application.yaml`과 `application-{local,dev,prod}.yaml`로 나눈다. `spring.profiles.default: local`을 두어, 프로파일을 지정하지 않은 실행은 local로 띄운다. 로컬에서 `bootRun`과 `importDrinks`를 따로 지정하지 않고 실행할 수 있기 때문이다. 기본값 없이 매번 지정하게 하는 안은 지정을 빠뜨리면 바로 실패해 더 안전하다. 하지만 로컬에서 실행할 때마다 지정해야 해서 택하지 않았다. 그 대신 dev와 prod는 `SPRING_PROFILES_ACTIVE`로 반드시 지정한다. local의 datasource와 Redis 값은 `spring.docker.compose.enabled=false`로 실행하거나 jar로 실행할 때를 위해 명시한 것이다. `bootRun`과 `importDrinks`에서는 `spring-boot-docker-compose`가 compose를 직접 띄우고, 그 연결이 이 값보다 우선한다. dev와 prod는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`로 주입한다. 6개 변수는 모두 필수이고, 비밀번호가 없는 Redis라도 `REDIS_PASSWORD`는 빈 값으로 넣어야 한다. 이 변수가 없으면 placeholder 문자열이 그대로 비밀번호가 되고, 기동은 성공한 뒤 처음 Redis를 쓸 때 실패하기 때문이다. 또 `spring.docker.compose.enabled: false`로 compose 연결을 끈다. `importDrinks`는 `bootRun`의 클래스패스를 그대로 써서 `spring-boot-docker-compose`가 함께 들어온다. 그래서 `SPRING_PROFILES_ACTIVE=dev ./gradlew importDrinks`로 dev에 적재할 때 로컬 compose 연결이 원격 연결을 덮어쓰지 않도록 이 설정이 필요하다. SQL 로그(`show-sql`, `format_sql`)는 local에서만 켜고 테스트에서도 켜지 않는다. `refresh-token.cookie-secure`는 local에서만 기본값이 false이고, dev, prod, test에서는 true로 고정한다. JWT, OAuth, CORS는 이미 환경변수로 주입하고 있어(비밀값은 `${ENV_VAR}`, 만료 시간과 CORS는 `${ENV_VAR:기본값}`) 프로파일 파일로 옮기지 않았다. `.env`는 Spring이 직접 읽지 않고 지금처럼 실행 환경에서 주입한다.

**(2026-10-18 결정, 이슈 #78)**: 안주 데이터는 시드 마이그레이션이 아니라 #76의 적재 러너로 넣는다. V7 마이그레이션은 `anju`에 컬럼과 제약을 추가하는 스키마 변경만 한다. 이슈 초안의 시드 마이그레이션은 맛을 규칙표로 계산한다는 #76 원칙과 맞지 않아 택하지 않았다. SQL INSERT로는 자바 규칙을 쓸 수 없고 계산 결과를 테스트할 수도 없기 때문이다. 안주는 사람이 고른 매핑표(`import/curated/anjus.csv`)에 이름, 식약처 식품영양성분DB(K-FIND) 식품코드, 주재료, 부재료, 주재료 계열, 형태를 적는다. 맛과 알레르기는 규칙표로 계산한다. 단맛은 당류 함량 구간으로, 무게감은 지방 함량 구간으로 정한다. 산미와 쓴맛은 영양성분으로 알 수 없어 재료 규칙표(`ingredients.csv`)의 맛 태그로 정하고, 탄산은 0으로 둔다. 알레르기는 주재료와 부재료 전체에서 계산하고, 매운맛은 주재료에서만 계산한다. 김치의 새우젓처럼 부재료는 알레르기로는 잡혀야 하지만 그 안주의 성격을 바꾸면 안 되기 때문이다. 안주마다 맛 값을 손으로 적는 안은 일관성을 검증할 수 없어 택하지 않았다. K-FIND는 이용허락범위 제한이 없어 추출한 영양성분 CSV를 커밋한다. 안주 규모는 150~300종으로 한다. K-FIND 음식에서 대표 안주를 고르고, K-FIND 가공식품에서 육포, 어포, 치즈 같은 안주류 제품을 더한다. 가공식품의 알레르기는 같은 품목제조보고번호로 식품안전나라 C002(품목제조보고 원재료)의 원재료명을 찾아, 재료 규칙표의 키워드로 계산한다. 안주는 `Drink`처럼 `(source, external_id)` 유니크 키로 식별한다. `AnjuSource`는 `SEED`와 `K_FIND`이고, `external_id`는 K-FIND 식품코드다. 업체마다 같은 제품명이 있어 이름을 식별자로 쓸 수 없기 때문이다. 시드 6행에는 `fried-chicken` 같은 읽을 수 있는 키를 넣었다. `uk_anju_name`은 추천 결과에 같은 이름의 안주가 두 번 나오지 않도록 유지하고, 제품의 표시명은 X-Wines처럼 업체명과 제품명을 합친다. 안주 10만 건 규모는 택하지 않았다. 음식 데이터에 그만한 안주가 없고, 요청마다 음료×안주 전체 쌍을 계산하는 구조에서 약 1억 쌍이 되기 때문이다. 300종이면 쌍이 약 31만 개로 지금의 약 50배다. 그래서 적재 후 k6로 측정하고, 병목이면 별도 perf 이슈로 다룬다. 시드 6행은 `pairing_feedback`이 FK로 참조하므로 id를 유지한 채 적재가 갱신한다.

**(2026-10-18 결정, 이슈 #107)**: `anjus.csv`에 `external_id`와 별도로 `kfind_code` 열을 둔다. `external_id`는 행을 식별하는 키이고, `kfind_code`는 K-FIND 영양성분을 찾는 조회 키다. 시드 6행은 `pairing_feedback`이 참조하는 행이라 `fried-chicken` 같은 키를 유지해야 하지만, 맛을 계산하려면 K-FIND 영양성분이 필요하다. 두 키가 하나의 열을 나누어 쓰면 SEED 행에서는 영양성분을 찾을 수 없고, 시드 키를 식품코드로 바꾸면 V7이 넣은 식별 키가 깨진다. 조회 키는 적재에서만 쓰므로 `anju` 테이블에는 컬럼을 추가하지 않는다. K_FIND 행의 `external_id`도 식품코드가 아니라 사람이 정한 `anju-001` 같은 키로 둔다. #78의 "`external_id`는 K-FIND 식품코드다"를 바꾼 결정이다. 식품코드는 사람이 고르는 선정 결과라 고칠 수 있는데, 식별 키로 쓰면 코드를 고칠 때마다 적재가 기존 안주를 새 안주로 본다. 그러면 같은 이름이 `uk_anju_name`에 걸리고, 고치지 않더라도 `pairing_feedback`이 참조하는 id가 바뀐다. #78이 이름을 식별자로 쓰지 않은 이유(업체마다 같은 제품명)도 이 키로 해결된다. 키는 한 번 정하면 바꾸거나 다시 쓰지 않고, 새 안주는 다음 번호를 쓴다. 적재가 소유한 K_FIND 안주 중 이번에 적재하지 않은 안주는 지운다. 매핑표에서 빠졌거나 맛을 정할 수 없게 된 안주가 예전 맛 값으로 계속 추천되면 안 되기 때문이다. 피드백이 참조하는 안주는 FK 때문에 지우지 못하므로 리포트에 남긴다. 남긴 안주는 예전 맛 값으로 추천에 계속 나오고, 같은 이름의 새 안주를 적재하면 `uk_anju_name`에 걸린다. 지금은 운영에 K_FIND 안주가 없어 이런 안주가 없으므로 숨김 처리를 미리 만들지 않는다. 리포트에 남긴 안주가 나오면 숨김 처리를 도입한다. 시드 안주는 마이그레이션이 소유하므로 지우지 않는다. Hibernate는 같은 트랜잭션에서 INSERT를 DELETE보다 먼저 실행하므로, 지우기를 바로 반영한 뒤 저장한다. 산미와 쓴맛은 재료 전체에서 가장 큰 값을 쓴다. 식초, 케첩, 커피처럼 소스와 양념인 부재료가 맛을 정하는 경우가 많기 때문이다. 매운맛만 주재료에서 계산한다. 규칙표에 없는 재료가 있는 안주는 적재에서 제외하고 리포트에 남긴다. 알레르기를 놓친 안주가 추천되는 것보다 빠지는 편이 안전하기 때문이다. 안주가 들어가면서 적재 범위가 음료를 넘어, 러너와 태스크 이름을 `ImportCatalogRunner`, `importCatalog`로 바꿨다. 이미 적용된 V5, V6 마이그레이션의 `importDrinks` 주석은 Flyway 체크섬이 바뀌므로 고치지 않는다.

**(2026-10-18 결정, 이슈 #107)**: 안주의 K-FIND 식품코드는 공공데이터포털 표준데이터(음식 15100070, 가공식품 15100066) CSV에서 `scripts/import/extract-kfind.py`로 고른다. 원본은 132MB가 넘어 커밋하지 않고, 안주에 쓰는 행만 뽑은 `import/kfind/nutrition.csv`를 커밋한다. 후보는 매핑표의 요리와 같은 요리인지를 먼저 보고 고른다. 출처의 정확도(분석 함량 우선, 프랜차이즈 제외)는 같은 요리 후보가 여러 개일 때만 쓴다. 순두부찌개에 출처가 앞선 김치 순두부를 골랐다가, 매핑표 재료(바지락)와 맞는 해물 순두부로 바꾼 일이 있어서다. 같은 요리가 없으면 주재료와 조리법이 같은 요리로 대신하고 여기에 남긴다. 대게찜은 꽃게찜, 베이컨팽이버섯말이는 베이컨떡말이구이, 웨지감자는 조리 전 냉동 웨지감자, 브리치즈구이는 꿀과 바게트를 뺀 브리치즈, 견과류모둠은 말린 과일이 섞인 하루견과, 양꼬치는 마라양꼬치, 소시지야채볶음은 소시지 케첩볶음으로 대신했다. 대신할 요리마저 맛이 달라지는 안주는 식품코드를 비워 적재에서 뺀다. 잘못된 맛 값으로 추천되는 것보다 빠지는 편이 정확하기 때문이다. 육사시미(살코기 부위 데이터 없음), 하몽멜론(멜론의 단맛이 빠짐), 양고기마라볶음, 조개찜, 굴보쌈(생굴과 식품코드가 겹침), 멍게, 떡꼬치, 염소치즈샐러드, 셰브르크래커, 멜론이 여기에 해당한다. 시드 마른안주와 과일안주도 맞는 데이터가 없어 V2의 값을 유지한다. 한 식품코드를 두 안주에 쓰지 않는다. 영양성분이 같으면 두 안주의 맛이 구분되지 않기 때문이다. 염소치즈 안주 2종이 빠져 `GOAT_CHEESE`는 맞는 안주가 0개다. 국물 요리는 100g당 지방이 국물로 희석되어 실제 느낌보다 무게감이 낮게 나오지만, 한국 술자리의 대표 안주라 유지한다. 추천에서 문제가 확인되면 따로 다룬다. 가공식품 안주 69종의 알레르기는 매핑표의 주재료와 부재료로만 계산했고, 제품 포장에 표기된 알레르기와 대조하지 않았다. 그래서 육포 양념의 대두처럼 제품 원재료에만 있는 알레르기를 놓칠 수 있다. 알레르기를 놓치면 그 알레르기를 입력한 사용자에게 해당 안주가 추천되므로 안전 문제다. #78이 정한 식품안전나라 C002는 원재료명 문장이라 키워드로 추정해야 하고, 국내 품목제조보고만 담아 수입품 18종은 조회되지 않아 택하지 않았다. 대신 포장의 알레르기 유발물질 표기를 그대로 담은 공공데이터포털 HACCP 제품이미지 및 포장지표기정보(15033307, 이용허락 제한 없음)로 대조한다. 대조 결과는 `nutrition.csv`의 `raw_materials`와 같은 방식으로 적재에 더한다. 운영 DB에 안주를 처음 적재하기 전에 이 대조를 마친다.

## pairing 추천 규칙

**(2026-10-07 결정, 이슈 #89)**: `Pairing.recommend`는 상위 결과에 같은 스타일(`drinkStyleId`)의 음료가 몰리지 않게 한다. 점수순 후보에서 스타일마다 최고점 조합을 하나씩 먼저 고르고, 스타일 수가 `RECOMMENDATION_LIMIT`보다 적으면 남은 자리를 점수순으로 채운다. 무알코올 보장은 그 뒤에 적용하고, 마지막에 점수순으로 한 번 정렬한다. 중복 기준을 `FlavorProfile`로 하지 않은 이유는 값이 완전히 같을 때만 걸러져서, 같은 스타일 안에서 맛이 조금씩 다른 와인(#77)은 막지 못하기 때문이다. 스타일당 하나만 두는 엄격한 상한도 택하지 않았다. 도수나 알레르기 조건으로 후보 스타일이 줄면 결과가 5개보다 적어지기 때문이다. 무알코올 보장이 넣은 조합은 알코올 음료와 스타일이 같을 수 있다. 무알코올 보장이 스타일 다양성보다 우선하기 때문이다. 정렬을 마지막에 하는 이유는 그 조합의 점수가 위의 결과보다 높을 수 있기 때문이다.

**(2026-10-15 결정, 이슈 #79)**: `Pairing.recommend`는 음료×안주×음악 전체 조합을 만들지 않는다. 조합 점수는 음료×안주 점수와 음악 점수의 합이다. 음료×안주 점수는 맛 유사도에서 두 감점을 뺀 값이고, 음악 점수는 자리 분위기 적합도다. 음악 점수는 음료, 안주와 무관하므로 음악마다 한 번만 계산한다. 후보는 다음 세 가지에 들 수 있는 음료×안주 쌍에 상위 5개 음악만 결합해 만든다. 전체 상위 5개, 스타일별 최고, 무알코올 최고(무알코올 옵션이 필요할 때)다. 각 경계값과 1e-9 이내로 가까운 후보도 함께 남긴다. 서로 다른 점수에 같은 값을 더하면 반올림으로 동점이 될 수 있다. 그러면 id 순서로 순위가 뒤집힐 수 있기 때문이다. 순위는 0으로 자르기 전 원점수로 매기고, 응답 점수만 0에서 자른다. 0으로 자른 뒤 순위를 매기면 나쁜 조합끼리의 순서가 id 순서로 정해지기 때문이다. 추천 사유는 최종 조합에만 계산한다. 도수 상한은 `DrinkRepository.findAllByAbvLessThanEqual`로 조회 단계에서 먼저 거르고, `Pairing`도 같은 규칙을 다시 검사한다. `drink.abv` 인덱스는 두지 않았다. 1,026행(17페이지)에서는 인덱스가 있어도 플래너가 Seq Scan을 골랐다. 강제로 인덱스를 쓰게 해도 두 방식의 차이가 0.1ms 안쪽이었다. X-Wines Full처럼 음료가 수만 건 이상으로 늘면 인덱스를 재검토한다. 로컬 K6 측정(조합 약 4.9만 개, 20 VU)에서 처리량은 38.8 req/s에서 322.4 req/s로, p95는 647ms에서 103ms로 개선됐다. #53 결정의 재검토 조건인 "자바 전건 계산이 병목이 될 때"는 vector 컬럼 전환 대신 이 방식으로 해결했다.

**(2026-10-18 결정, 이슈 #107)**: 안주 적재 후 음료×안주 쌍이 179,550개(#79의 약 3.7배)로 늘자, 로컬 K6(M1 8코어, 20 VU)에서 처리량이 29.0 req/s, p95가 877ms로 떨어졌다. 원인은 요청마다 쌍 전체를 `ScoredDrinkAnju`로 만들고, 상위 5개 경계를 구하려고 전체를 정렬한 것이었다. 안주의 알레르기 확인도 음료마다 반복했다. `Pairing`은 필터를 쌍 반복 밖에서 한 번만 적용하고, 점수를 배열에 담아 세 선정 규칙의 경계를 정렬 없이 한 번에 구한 뒤 경계를 넘는 쌍만 객체로 만든다. 맛 유사도는 배열 없이 정수 제곱 합으로 계산해 결과가 이전과 비트 단위로 같다. 기존 구현과 무작위 18,000건의 추천이 같은지 임시 테스트로 확인했고, 처리량은 268.6 req/s, p95는 114ms, p99는 166ms가 됐다. 쌍이 수백만 개로 늘면 요청마다 전체를 계산하는 구조 자체를 재검토한다.

**(2026-10-18 결정, 이슈 #78)**: 안주는 X-Wines `Harmonize` 상위 20개(Slim 기준 Poultry 582건부터 Goat Cheese 43건까지)를 분류 기준으로 쓰되, 안주와 Harmonize의 관계는 저장하지 않는다. 안주에는 단일값 속성 3개만 둔다. 주재료 계열(`AnjuBase`), 형태(`ServingStyle`: MAIN, APPETIZER, SNACK), 매운맛 여부(`spicy`)다. `Harmonize` enum은 태그마다 조건 하나로 `matches(Anju)`를 판정한다. Harmonize는 주재료(Poultry, Pork 등), 맛(Spicy Food), 형태(Appetizer, Snack)처럼 서로 다른 기준이 섞인 어휘라서, 태그 하나가 기준 하나만 묻기 때문이다. 그래서 닭발은 POULTRY, MAIN, 매움이라는 속성으로 Poultry와 Spicy Food에 함께 맞는다. `anju_harmonize` 다대다 테이블은 택하지 않았다. 안주마다 태그를 몇 개 붙일지 판단이 갈리고, 잘못된 조합을 제약으로 막을 수 없으며, 매핑을 바꿀 때마다 데이터를 다시 고쳐야 하기 때문이다. 안주마다 대표 Harmonize 하나만 두는 안도 택하지 않았다. 닭발에서 Poultry와 Spicy Food 중 하나를 버려야 해서, 매운 요리가 많은 한국 안주에서는 손실이 크다. 판정 조건은 태그마다 보는 속성이 달라 값이 아니라 로직이다. 그래서 #77 기준에 따라 CSV 규칙표가 아니라 enum의 행위로 둔다. 맞는 한국 안주가 없는 Veal은 BEEF로 흡수하고, Game Meat은 오리를 포함한 GAME 계열로 판정한다. 주재료가 둘인 요리는 대표 계열 하나를 고른다. 판정 규칙 구현은 적재 러너와 함께 #107로 나눴고, 와인 Harmonize 적재와 점수 반영은 그 뒤의 후속 이슈로 둔다. 맥주와 소주에는 Harmonize가 없어 보너스를 주면 추천이 와인으로 쏠린다. 또 Poultry가 와인의 58%에 붙어 있어 태그별 희소도 가중을 따로 설계해야 한다. 21위 이하 태그(Blue Cheese, Chicken 등)는 와인 쪽을 적재할 때 20개 중 하나로 흡수한다. 대표 계열을 하나만 고르는 방식 때문에 추천 품질이 떨어지는 사례가 쌓이면 재검토한다.

**(2026-10-20 결정, 이슈 #105)**: 음료×안주 점수에 Harmonize 점수와 도수 대비 점수를 더한다. 음료의 음식 계열 중 하나라도 안주에 맞으면 0.1을 더한다. 0.1은 가벼운 안주 감점과 같은 크기다. 도수 15% 이상인 음료와 무게감 4 이상인 안주에는 `0.2 × (무게감 − 3) / 2`를 더한다. 도수가 높은 술이 기름진 안주를 씻어 주는 효과를 반영한 것이다. 0.2는 소주와 무게감 5 안주의 무게감 차이로 깎이는 맛 유사도 점수(약 0.12)보다 커야 소주가 기름진 안주를 가벼운 안주보다 앞에 두기 때문에 정했다. 15%는 희석식 소주(16~17%)와 증류주, 주정강화 와인부터 해당하고 일반 와인 대부분은 해당하지 않는 경계다. 음료의 음식 계열은 `drink.harmonizes` 배열 컬럼(V8)에 담는다. 와인은 X-Wines `Harmonize` 태그를 `wine-harmonize.csv`로 20개 계열에 옮긴다. 규칙표에 없는 태그가 있는 와인은 맛을 정하지 못한 와인처럼 적재하지 않고 리포트에 남긴다. 맥주는 BJCP 스타일별 규칙표 `beer-harmonize.csv`로 정한다. 근거는 Brewers Association 맥주·음식 표의 Suggested Foods 열이다. Cheese, Dessert 열은 모든 스타일에 하나씩 채워져 있어 스타일을 구분하지 못하므로 쓰지 않았다. 시드 소주는 회식 설문(삼겹살과 소주 65%)과 외국인 고객 설문(고기와 소주 1위)에 근거해 V8에서 PORK, BEEF를 넣는다. 막걸리, 위스키, 하이볼, 무알코올 음료는 구조화된 근거를 찾지 못해 음식 계열을 비워 두었다. 근거 없는 값으로 추천되는 것보다 점수를 받지 못하는 편이 정확하기 때문이다. #78이 걱정한 태그별 희소도 가중은 넣지 않았다. 카탈로그 전체로 시뮬레이션해 보니 맥주와 소주에도 음식 계열을 주면 안주별 최고 음료가 와인인 비율이 늘지 않았다. 태그의 희소도는 와인끼리 구분하는 정보일 뿐 그 와인과 안주가 얼마나 잘 맞는지를 나타내지 않는다. 안주별 최고 음료는 여전히 대부분 와인이고 맥주는 0건이다. 이는 Harmonize 이전부터 있던 문제로, 와인 999종이 맛 공간을 촘촘히 채우고 맥주는 탄산 축에서 감점을 받기 때문이다. 탄산 축은 #106에서 다룬다.

**(2026-10-22 결정, 이슈 #106)**: 음료×안주 맛 유사도에서 탄산 축을 뺀다. 안주의 탄산은 적재 규칙상 늘 0이라(#78), 탄산 축은 안주끼리 구분하지 못하고 맥주와 하이볼을 모든 안주에서 멀어지게 할 뿐이었다. `FlavorProfile.similarity()`를 쓰는 곳이 음료×안주 비교뿐이라 음료×안주 전용 유사도를 따로 두지 않았다. 탄산은 산미와 함께 기름진 안주를 씻어 주는 대비 점수로만 반영한다. 탄산이나 산미가 4 이상인 음료와 무게감 4 이상인 안주에는 `0.2 × max(탄산, 산미) / 5 × (무게감 − 3) / 2`를 더하고, 추천 이유 `CARBONATION_ACIDITY_CUTTING`을 붙인다. 탄산과 산미는 같은 효과라 더하지 않고 강한 쪽만 본다. 문턱 없이 모든 음료에 비례해 주는 안은 택하지 않았다. 산미 1~2인 음료도 점수를 받아 추천 이유를 붙일 기준이 없어지기 때문이다. 도수 대비(#105)와 둘 다 받는 음료에는 큰 쪽만 준다. 같은 기름기를 씻는 효과를 두 번 세지 않기 위해서다. 가중치 0.2는 도수 대비와 같은 크기다. 그래서 탄산 5인 맥주가 기름진 안주에서 소주와 같은 대비 점수를 받는다. 축이 4개로 줄어 정규화 분모가 125에서 100이 되면서 같은 거리의 유사도가 낮아지고, 유사도 차이는 약 1.12배 커졌다. 카탈로그 179,550쌍 중 `MATCH_THRESHOLD`(0.7) 이상인 쌍은 14,597쌍(8.1%)에서 9,192쌍(5.1%)으로 줄었다. 맥주와 하이볼 등 큐레이션 음료는 1쌍에서 533쌍으로 늘었고, 와인은 8.3%에서 4.8%로 줄었다. 새로 매칭되는 조합(밀맥주와 치킨류, 하이볼과 해산물)이 페어링 상식에 맞아 #74처럼 0.7을 유지했다. 가산점과 감점의 크기(0.1, 0.2)는 바꾸지 않았다. 라거(카스 프레시)에서 후라이드치킨은 카탈로그 173개 중 117위에서 공동 9위가 됐다. 1위는 무게감 5인 팝콘, 볶음땅콩, 견과류모둠, 콜드컷플래터의 동점이다. 그래서 골든 세트는 후라이드치킨의 순위 대신 상위 3위가 탄산 대비와 Harmonize 사유를 함께 갖는지 검증한다. 산미 4 이상인 와인 757종도 대비 점수를 받는다. 그래서 Louis Jadot Beaujolais의 상위 안주가 깐풍기, 두부김치에서 대창구이, 삼겹살구이, 소막창구이로 바뀌었다. develop과 jar를 번갈아 띄워 로컬 K6(20 VU)로 3회씩 측정한 처리량은 평균 368.8 req/s에서 358.2 req/s였다. 두 쪽 측정 범위가 겹쳐 차이는 노이즈 수준이고, p95는 약 72ms로 같다.

**(2026-10-20 결정, 이슈 #105)**: 음료의 음식 계열은 `anju_allergy_type` 같은 컬렉션 테이블이 아니라 `drink.harmonizes` `VARCHAR(255)[]` 컬럼에 담는다. 허용 값은 `ck_drink_harmonizes` 제약(`<@ ARRAY[...]`)으로 막는다. 추천은 요청마다 음료 1,026종 전체를 읽기 때문이다. 처음에는 `drink_harmonize` 테이블을 fetch join으로 읽었다. 그러자 음료 행이 계열 수만큼 약 4.6천 행으로 늘고 요청마다 컬렉션 1,026개가 만들어져, 로컬 K6(20 VU)에서 처리량이 기준선(약 270 req/s)의 절반 아래인 약 117 req/s로 떨어졌다. 음료와 `(drink_id, harmonize)` 행을 쿼리 두 개로 따로 읽어 합치는 방식도 시도했다. 하지만 JFR로 보니 Hibernate가 행 약 4.6천 개를 객체로 만드는 비용이 음료 조회만큼 들어, 요청당 CPU가 기준선보다 약 19% 많았다. 배열 컬럼은 음료 행과 함께 한 번에 읽혀 추가 쿼리와 행 객체가 없다. 컬렉션 테이블과 달리 행 단위 FK와 PK가 없지만, 계열은 음료에만 딸린 값 목록이라 잃는 무결성이 없다. 안주의 알레르기는 안주가 173종뿐이라 컬렉션 테이블을 유지한다. 쌍마다 음식 계열을 집합으로 비교한 것도 추천 계산 시간을 호출당 2.6ms에서 4.8ms로 늘렸다. 그래서 음료와 안주는 만들 때 음식 계열을 비트 집합으로 한 번 계산해 두고, 쌍마다 비트 AND 한 번으로 판정한다. 배열 컬럼으로 바꾼 뒤 #105 이전의 develop과 같은 DB, 같은 스크립트로 서버를 번갈아 띄워 로컬 K6(20 VU)로 3회씩 측정했다(2026-10-22). 처리량은 평균 250.6 req/s에서 217.0 req/s로 약 13% 줄었다. p95는 118ms에서 127ms로, p99는 163ms에서 165ms로 늘었다. 브랜치의 3회 측정값은 모두 develop의 3회 측정값보다 낮았다. 이 차이는 #105가 더한 음식 계열 점수, 도수 대비 점수 계산과 배열 컬럼 읽기를 합친 비용이다. fetch join일 때처럼 처리량이 절반 아래로 떨어지는 문제는 사라졌다.

## DDD 핵심 원칙

- **DDD 기반**: 도메인 로직은 도메인 객체 메서드에 위치 (Service는 오케스트레이션만).
- **`@Service` vs `@Component`**: `@Service`는 `*UseCase` 구현체(비즈니스 로직 실행자)에 붙인다. `@Component`는 스케줄러·이벤트 리스너 같은 기술적 wiring에만 쓴다. 여러 도메인을 조율하는 비즈니스 오케스트레이터 용도로는 쓰지 않는다.
- **UseCase 단위 분리 (Facade 대체)**: 1 `*UseCase` 인터페이스 = 1 `*Service` 구현체가 기본. 로그인/재발급/로그아웃처럼 트리거·부수효과가 다른 흐름을 하나의 Service가 묶어 처리하지 않는다.
  - **판단 기준**: 메서드들이 부수효과·외부 연동 없이 유사한 단순 조회/CRUD라면 Query/Command 레벨 유지 가능 (예: `MemberQueryService`/`MemberCommandService`). 트리거·부수효과·외부 연동이 서로 다른 별개의 흐름이라면 UseCase 단위로 반드시 분리 (예: `RefreshUseCase`/`RefreshService`, `LogoutUseCase`/`LogoutService`).
- **Presentation → Application 변환**: Request DTO의 `toCommand()`로 Command 객체 생성, 도메인은 원시값만 받음.
- **인증 식별자 추출**: `@AuthenticationPrincipal String memberId`.
- **RTR(Refresh Token Rotation)**: 토큰 탈취 감지, 누락 시 단순 거부(Tombstone 없음). 리프레시 토큰은 httpOnly+Secure+SameSite 쿠키로 전달, 액세스 토큰은 응답 바디.
- **쿠키 기반 인증 제약**: CORS `Access-Control-Allow-Credentials: true` 필요 → Origin 와일드카드(`*`) 사용 불가, 허용 오리진 명시 필수. CSRF는 리프레시 토큰 쿠키에 `SameSite=Strict` 적용으로 방어(별도 CSRF 토큰 없음). 쿠키 `Path`는 재발급 엔드포인트로 제한 권장.
- **Rate Limiting 미적용 (2026-10-08 결정, 이슈 #83)**: 앱 단 IP별 rate limit(`RateLimitFilter`)을 제거했다. signup·login은 일회용 OAuth 인가 코드를, refresh는 추측할 수 없는 UUID 리프레시 토큰을 받으므로 무차별 대입이 성립하지 않는다. 대량 요청 방어는 요청이 앱에 닿기 전에 막는 LB/WAF(예: Cloud Armor)가 맡는 편이 낫다. 필터를 유지하려면 프록시 뒤 IP 식별과 `INCR`/`EXPIRE` 원자화(Lua)를 함께 고쳐야 했는데, 그 비용에 비해 실익이 작아 택하지 않았다. 잘못된 인가 코드로 반복 호출해 카카오·구글 API 호출 한도를 소모시키는 공격이 관측되거나, 비밀번호 로그인처럼 대입 가능한 입력을 받는 엔드포인트가 생기면 재검토한다.
- **클라이언트 IP 식별 (2026-10-08 결정, 이슈 #83)**: 클라이언트 IP는 감사 로그(`AuditLogRecorder`)의 `ip` 필드에만 쓰인다. `server.forward-headers-strategy: native`로 Tomcat `RemoteIpValve`를 켜고, `server.tomcat.remoteip.internal-proxies`(`TRUSTED_PROXIES` 환경변수, 기본값은 Spring Boot의 사설 IP 대역)에 든 프록시가 보낸 `X-Forwarded-For`만 반영한다. 그래서 IP는 어디서든 `HttpServletRequest.getRemoteAddr()`로 구하고, 별도 IP 추출 유틸을 두지 않는다. `framework` 전략(`ForwardedHeaderFilter`)은 헤더를 무조건 신뢰해 위조에 열려 있어 택하지 않았다. 신뢰 프록시 목록이 실제 LB 대역과 어긋나면 헤더가 무시되어 LB IP가 남을 뿐 위조에 열리지는 않으므로, 동작 검증은 테스트가 아니라 배포 시 `TRUSTED_PROXIES`를 넣고 감사 로그를 확인하는 것으로 한다. MockMvc는 Tomcat을 거치지 않아 이 동작을 검증할 수 없고, 실제 포트를 띄우는 테스트는 우리 코드가 아니라 Tomcat 구현을 검증하게 되어 두지 않았다. IP를 인가·차단 판단에 쓰기 시작하면 테스트 추가를 재검토한다.
- **YAGNI**: 안 쓰는 에러 코드·검증기·추상화는 주저 없이 제거.

## 응답 포맷

`StandardResponse<T>`(`common.response`)가 모든 응답을 감싼다. 필드는 `boolean ok`, `int status`(HTTP 상태 코드), `T data`로 확정되어 있다. 성공 응답은 `StandardResponse.success(status, data)`(반환값 없는 성공은 `data` 없는 오버로드), 에러 응답은 `StandardResponse.fail(status, ErrorResponse)` 정적 팩토리로 생성한다.

## 정적분석 도구 미도입 사유

Konsist/ArchUnit 같은 아키텍처 자동검증 도구는 도입하지 않기로 했다. 단일 모듈 구조라 패키지 규칙 위반을 코드리뷰(`code-reviewer` 에이전트의 체크리스트 기반 수동 검토)로 잡는 게 더 실용적이라고 판단했기 때문이다. 계층 의존 방향처럼 판단이 섞이는 규칙은 이 방식으로 검증하고, EOF 개행·코드 스타일처럼 완전히 결정론적인 것만 스크립트/포맷터로 강제한다.
