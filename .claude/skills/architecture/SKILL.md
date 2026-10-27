---
name: architecture
description: Load when designing layer structure. UseCase/Service/Api/Controller 분리, 계층별 타입 어휘, DTO↔Command 변환, 응답 포맷, 크로스 도메인 참조, 프로젝트 구조, 트랜잭션 경계, DDD 핵심 원칙, 영속성(PostgreSQL/pgvector, Flyway, 인덱스·제약, 프로파일 설정, 카탈로그 적재, persistence.md), pairing 추천 점수 규칙(pairing.md), gathering 정산 규칙(gathering.md), 메시지 큐(RabbitMQ) 사용 범위.
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
├── catalog/               # 음료, 안주, 음악 무드 카탈로그와 적재 (이슈 #139, 코드 이동은 #140)
├── pairing/               # 술, 안주, 음악 페어링 추천 (이슈 #45). 아래 카탈로그 코드는 #140에서 catalog로 옮긴다
│   ├── domain/            # 순수 도메인 모델(Drink, DrinkStyle, Anju, MusicMood, Pairing), FlavorProfile/MoodProfile/Occasion 값 객체, PairingFeedback, *Repository 인터페이스, 규칙표로 맛을 계산하는 FlavorRule/WineFlavorRule/AnjuRule, 추천 후보를 계산하는 DrinkAnjuScores/CandidateThresholds, 적재 원본을 읽는 *Reader 인터페이스(reader)
│   ├── application/       # *UseCase 인터페이스 + *Service 구현체 (예: PairingQueryService, PairingCommandService, Query/Command 레벨. 파일을 읽는 적재는 UseCase 단위로 분리: ImportDrinkStyleService, ImportCuratedDrinkService, ImportXWineService, ImportAnjuService)
│   ├── presentation/      # Controller, *Api, 수동 적재 진입점 ImportCatalogRunner(runner)
│   └── infrastructure/    # DrinkRepository/AnjuRepository/PairingFeedbackRepository 구현체, JPA 엔티티, *Reader 구현체(reader: JsonBjcpStyleReader, CsvFlavorRuleReader, CsvCuratedDrinkReader, CsvXWineReader, CsvWineFlavorRuleReader, CsvBeerHarmonizeRuleReader, CsvCuratedAnjuReader, CsvAnjuNutritionReader, CsvAnjuRuleReader)
├── gathering/             # 술자리 정산 (이슈 #122, 구현은 #123~#131)
├── common/                # 공유 커널: 공통 예외(BusinessException 등, common.exception), 응답 포맷(StandardResponse), BaseTimeEntity, CQRS 트랜잭션 애노테이션(@CommandService/@QueryService, common.annotation)
└── global/                # 기술 부트스트랩: Security/JPA/Redis/Swagger 설정 (config), 인증 필터·핸들러 (security)
```

> 도메인 모델과 JPA 엔티티는 완전히 분리한다. `domain`은 영속성 기술을 전혀 알지 못한다. Domain↔JPA 변환은 기본적으로 `*JpaEntity`의 정적 팩토리 메서드(`from(도메인객체)`, `toDomain()`)로 처리한다. 같은 변환을 여러 곳에서 재사용하거나 필드 매핑이 단순 대입을 넘어 계산·검증을 포함하게 되는 시점에만 별도 `*Mapper` 클래스로 분리한다 (YAGNI: 호출자가 하나뿐인 단순 매핑에 별도 클래스를 미리 만들지 않는다).
> 도메인은 총 5개(`member`, `auth`, `catalog`, `pairing`, `gathering`)다(#139에서 변경). 3번째 도메인이 늘어난 시점에 `shared` 패키지 도입 여부를 재검토했다. `pairing`은 아직 `member`나 `auth`의 `*UseCase`에 의존하지 않아 별도 `shared` 패키지를 두지 않기로 했다. 크로스 도메인 조율은 대상 도메인의 `*UseCase` 인터페이스를 직접 의존하는 것으로 충분하다. 앞으로 어느 도메인이든(#122에서 변경) 하나의 `*Service`가 크로스 도메인 `*UseCase`를 2개 이상 의존하게 되는 시점에 다시 재검토한다. 4번째 도메인 `gathering`을 추가하면서 이 조건을 다시 확인했다(2026-10-25, 이슈 #122). 현재 설계(#123~#131) 기준으로 `gathering`은 크로스 도메인 `*UseCase`로 `pairing`의 `SubmitPairingFeedbackUseCase` 하나만 의존하므로(#131) 재검토 조건에 해당하지 않는다. 5번째 도메인 `catalog`를 분리하면서 이 조건을 다시 확인했다(2026-10-27, 이슈 #139). #140 설계 기준으로 `PairingQueryService`는 `catalog`의 `GetCatalogCandidatesUseCase` 하나만, `PairingCommandService`는 `VerifyCatalogItemsUseCase` 하나만 의존하므로 재검토 조건에 해당하지 않는다.

**(2026-10-27 결정, 이슈 #139)**: `pairing`에서 카탈로그를 `catalog` 도메인으로 분리한다. `catalog`는 음료, 안주, 음악 무드가 무엇이고 맛과 분위기 값이 얼마인지를 책임진다. `Drink`, `DrinkStyle`, `Anju`, `MusicMood`, 적재 규칙표, `*Reader`, 적재 러너가 여기에 속한다. `pairing`은 자리에 맞는 조합을 고르는 일을 책임진다. `Occasion`, 점수 계산, `Pairing`, `PairingFeedback`이 여기에 속한다. 근거는 두 가지다. 첫째, 적재 코드는 출처 데이터가 바뀔 때 바뀌고, 추천 코드는 점수 규칙이 바뀔 때 바뀐다. 분리 전 `pairing/domain` 파일 75개 중 34개가 적재용이었다. 둘째, 추천은 서비스의 핵심 도메인이고 카탈로그는 이를 받쳐 주는 지원 도메인이다. `pairing` 안에서 패키지만 나누는 안은 택하지 않았다. 크로스 도메인 규칙이 적용되지 않아 경계가 이름뿐이기 때문이다. 의존 방향은 `pairing → catalog` 한 방향이다. `FlavorProfile`과 `MoodProfile`은 두 도메인이 각자 가진다. `catalog`의 것은 적재 규칙이 계산한 값을 담는다. `pairing`의 것은 유사도, 무게감, 분위기 적합도 같은 점수 계산 메서드를 가진다. 하나를 `common`에 두는 안은 택하지 않았다. `common`은 기술 공통 요소만 두는 곳이다. 또 점수 계산 메서드를 공유하면 `catalog`가 추천 규칙에 묶인다. `Drink.cutsRichnessOf`처럼 엔티티에 있던 점수 계산 메서드와 `Anju.conflictsWith` 같은 추천 필터는 `pairing`의 후보 모델로 옮긴다. 음식 계열(`Harmonize`) 판정은 안주 속성으로 정해지는 분류라서 `catalog`에 둔다. `catalog`는 UseCase 두 개를 연다. `GetCatalogCandidatesUseCase`는 도수 상한 조건으로 추천 후보를 일괄 조회한다. `VerifyCatalogItemsUseCase`는 피드백 대상이 있는지 확인한다. 둘 다 부수효과 없는 단순 조회라서 Query 레벨 `CatalogQueryService` 하나가 함께 구현한다. 후보 `*Result`에는 식별자, 표시 정보, 스타일 id, 맛과 분위기 축, 도수, 알레르기 유형, 음식 계열 판정 결과 등 추천에 필요한 값을 원시 값과 enum으로 담는다. `pairing`은 이를 자기 후보 모델로 바꾼다. 테이블과 FK는 바꾸지 않는다. 결정 문서의 위치는 design-decision 스킬의 #134 기준을 따른다. `catalog`에 적재 외의 실행 중 규칙이 생기면 그때 `catalog.md`를 만든다.

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
- **(2026-10-27 결정, 이슈 #139)**: `src/main` 코드는 다른 도메인의 `domain` 패키지에서 enum만 참조하고, 그 enum의 메서드는 호출하지 않는다. 엔티티와 값 객체는 참조하지 않는다. enum을 값으로 넘기기만 하면 상대 도메인의 규칙에 의존하지 않는다. `auth`가 `member`의 `SocialProvider`를 `*Result`와 외부 연동 응답에 담는 것과, `gathering`이 `pairing`의 `RelationshipType`, `MoodType`을 피드백 Command에 담는 것이 그 예다. enum의 메서드를 호출하면 상대 도메인의 규칙을 직접 쓰게 된다. 그래서 `pairing`은 `catalog`의 `Harmonize.matches()`를 호출하지 않고, `catalog`가 넘긴 판정 결과를 쓴다. 엔티티와 값 객체까지 막는 이유는 `catalog`와 `pairing`이 같은 이름의 `FlavorProfile`을 각자 가지기 때문이다. 이 규칙이 없으면 잘못 import해도 컴파일과 리뷰를 모두 통과한다. 메서드가 있는 enum의 참조까지 막는 안은 택하지 않았다. 상대 도메인의 UseCase가 받는 enum을 쓸 수 없게 되고, enum의 메서드를 다른 곳으로 옮기면 `switch` 분기만 늘어나기 때문이다. enum을 모두 막는 안도 택하지 않았다. 같은 값 목록을 도메인마다 복제하고 변환 코드만 늘어나기 때문이다. 테스트는 다른 도메인의 상태를 준비하거나 검증하려고 엔티티와 Repository를 참조할 수 있다.

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

DB 선택, Flyway 마이그레이션, 인덱스와 제약, 프로파일별 설정, 카탈로그 적재 러너에 관한 결정은 [`persistence.md`](persistence.md)에 있다. 엔티티와 `*JpaEntity` 매핑, `db/migration`의 마이그레이션 파일, `application*.yaml`, 적재 러너나 적재 규칙표를 바꿀 때 읽는다. 다만 적재 규칙표라도 추천 점수 때문에 정한 Harmonize 규칙표(`wine-harmonize.csv`, `beer-harmonize.csv`)와 `drink.harmonizes` 컬럼(#105), 안주 무게감 보정 규칙표(`anju-serving-richness.csv`, #117)에 관한 결정은 `pairing.md`에 있다. 적재 코드가 `catalog` 도메인으로 옮겨져도(#139) 이 위치는 바뀌지 않는다.

## pairing 추천 규칙

`Pairing.recommend`의 점수 규칙(맛 유사도, 감점, 대비 점수, Harmonize), 후보 선정과 스타일 다양성, 추천 성능, 피드백 집계에 관한 결정은 [`pairing.md`](pairing.md)에 있다. `pairing` 도메인의 추천 로직이나 점수에 쓰는 값을 바꿀 때 읽는다. Harmonize 규칙표, `drink.harmonizes` 컬럼, 안주 무게감 보정 규칙표를 바꿀 때도 읽는다. 점수에 쓰는 맛 값 자체를 계산하는 규칙표(`FlavorRule`, `WineFlavorRule`, `AnjuRule`, #76~#78, #107. `AnjuRule`의 형태 보정(#117)은 빼고)와 `Drink`, `DrinkStyle` 애그리거트 분리(#75)는 `persistence.md`에 있다.

## gathering 정산 규칙

`gathering` 도메인의 애그리거트 경계(`Gathering`, `Settlement`), 분담 방식, 입금 상태 전이, 발송 시각과 리마인드, 개인정보 암호화와 단계적 파기, 익명 페어링 피드백에 관한 결정은 [`gathering.md`](gathering.md)에 있다. `gathering` 도메인의 모델, 정산 흐름, 개인정보 처리를 바꿀 때 읽는다. `pairing_feedback.member_id`처럼 `gathering` 때문에 바뀌는 `pairing` 쪽 규칙을 다룰 때도 읽는다. 큐를 쓰는 범위는 메시지 큐 절에 있다.

## 메시지 큐 (RabbitMQ)

**(2026-10-25 결정, 이슈 #122)**: 메시지 큐는 RabbitMQ를 쓴다. 쓰기로 한 곳은 `gathering`의 정산 메시지와 리마인드 지연 발송(#126, #127)뿐이다. 도입은 #126에서 한다. 도입 근거는 세 가지다. 첫째, 발송 시각이 모임과 참여자마다 달라 예약이 시각별로 흩어진다. 둘째, 서버가 재시작돼도 예약이 유실되면 안 된다. 셋째, 외부 발송 장애가 API 서버로 번지지 않아야 하고 재시도와 DLQ가 필요하다. DB에 발송 예정 시각을 두고 스윕하는 안은 택하지 않았다. 발송은 분 단위로 정확해야 해서 스윕 주기를 짧게 둬야 한다. 그러면 DB 폴링이 계속 생긴다. 재시도, DLQ, 외부 장애 격리도 직접 만들어야 한다. Redis를 지연 큐로 쓰는 안도 택하지 않았다. 지금 Redis는 토큰 저장소라서, 지연 큐로 쓰려면 영속성 설정, 재시도, DLQ를 모두 직접 구현해야 한다. 메시지에는 id만 담는다. 컨슈머는 발송 직전에 상태를 다시 조회하고 멱등하게 처리한다. 큐에 개인정보가 남지 않게 하고, 그사이 입금되거나 파기된 건을 보내지 않기 위해서다. 정산 건이 커밋된 뒤 메시지 발행이 실패해 예약이 사라지는 경우의 대책과 컨슈머가 지난 예약을 알아보는 방식은 지연 발송 방식(메시지별 TTL의 head-of-line 문제 등)과 함께 #126에서 정한다. 리마인드 횟수 제한 같은 도메인 규칙은 큐 설정이 아니라 애그리거트가 판단한다. 큐를 쓸 근거가 없는 곳에는 쓰지 않는다. 개인정보 파기(#128)는 주기적 스윕으로 처리한다. 지연 메시지가 유실되면 데이터가 영구히 남고, 파기는 한 시간 늦어도 문제가 없기 때문이다. 페어링 피드백 집계(#130)도 큐를 쓰지 않는다. 근거는 `pairing.md`에 있다. 다른 곳에 큐를 새로 쓰려면 위 세 근거 중 하나에 해당하는지 먼저 따진다.

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
