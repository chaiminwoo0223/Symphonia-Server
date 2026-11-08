---
name: testing
description: Load when writing or modifying tests. JUnit5/Mockito/Testcontainers, given-when-then 패턴, 계층별 테스트 전략, 이중 @Nested 구조, 명명 규칙, Fixture/Helper 패턴, 테스트 컨테이너 공유, 페어링 골든 세트, 부하테스트(K6).
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Test Rules

이 문서는 현재 규칙만 담는다. 근거와 변경 이력은 `docs/decisions/testing.md`에 있다.

## 테스트 라이브러리

JUnit 5, Mockito, Testcontainers를 쓴다.

## 계층별 테스트 범위

| 계층 | 검증 대상 | 베이스 클래스 |
|---|---|---|
| `infrastructure` (JPA Repository) | JPA 쿼리 반환값 정확성 | `RepositoryTest` (`@DataJpaTest` + Testcontainers) |
| `infrastructure` (Redis Repository) | Redis 저장/조회 정확성 | `RedisRepositoryTest` (`@DataRedisTest` + Testcontainers) |
| `application` (Service) | 비즈니스 로직 수행 여부 | `UnitTest` (`MockitoExtension`) |
| `presentation` (Controller) | 요청/응답, 인증/인가, 예외 매핑 | `IntegrationTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`) |

- `domain` 계층 테스트는 만들지 않는다. 도메인 로직은 그 로직을 쓰는 `*Service` 테스트로 검증한다(예: `Pairing.recommend`는 `PairingQueryServiceTest`). 일시적으로 도메인 테스트가 필요하면 커밋하지 않고 작업이 끝나면 삭제한다.
- `PairingQueryServiceTest`는 `GetCatalogUseCase`를 mock하지 않고, mock Repository를 넣은 실제 `CatalogQueryService`로 `@BeforeEach`에서 SUT를 조립한다. `CatalogQueryService` 자체는 `CatalogQueryServiceTest`가 검증한다.

## 테스트 컨테이너

- Postgres와 Redis 컨테이너는 `TestcontainersConfiguration`의 static 필드로 테스트 JVM에서 한 번만 띄운다. 빈으로 등록하지 않고 `DynamicPropertyRegistrar`로 접속 정보만 넘긴다. 빈으로 두면 컨텍스트 캐시가 오래된 컨텍스트를 닫을 때 공유 컨테이너도 멈춘다.
- RabbitMQ 컨테이너는 `RabbitTestcontainersConfiguration`의 빈으로 두어 `IntegrationTest` 컨텍스트마다 따로 띄운다. 공유하면 캐시된 다른 컨텍스트의 리스너가 메시지를 가져간다.
- 컨텍스트끼리 DB와 Redis를 같이 쓰므로 테스트는 데이터를 남기지 않는다. 트랜잭션 롤백을 쓰지 않는 테스트(`Propagation.NOT_SUPPORTED`)는 `@AfterEach`에서 직접 지운다.
- 테스트 커넥션 풀은 컨텍스트마다 최대 4개(`application-test.yaml`)이고, Postgres 컨테이너의 `max_connections`는 200이다. 컨텍스트 조합(`@Import`, `@MockitoBean`)이 늘어 연결 수가 부족하면 이 두 값을 함께 조정한다.

## 명명 규칙

각 이름은 자기 레벨에서만 새로운 정보를 더한다. 상위 레벨이 이미 표현한 내용을 하위 레벨에서 다시 쓰지 않는다. 사람이 읽을 설명은 모두 `@DisplayName`(한국어)에 담고, 클래스명과 메서드명에는 한국어를 쓰지 않는다.

| 대상 | 규칙 | 예 |
|---|---|---|
| 테스트 클래스 | `*Test` (통합 테스트는 `*IntegrationTest`) | `MemberServiceTest` |
| 메서드 레벨 `@Nested` | 테스트 대상 동작(메서드/쿼리/엔드포인트)을 나타내는 PascalCase 명사. 접두어 없음 | `GetMember`, `FindByEmail`, `CreateMember` |
| 조건 레벨 `@Nested` | `When` + 조건. 조건이 여러 갈래로 나뉠 때만 쓴다 | `WhenMemberNotFound` |
| 테스트 메서드 | `should` + 기대동작. 예외 케이스는 조건 레벨이 있어도 검증하는 예외 타입명을 그대로 담는다(`shouldThrowException()` 금지). 조건 레벨 없이 단일 케이스면 `should[기대동작]When[조건]`을 쓰되, 예외명이 조건을 드러내면 `When[조건]`을 붙이지 않는다 | `shouldThrowMemberNotFoundException()`, `shouldReturnMemberResultWhenMemberExists()` |
| 복수 표기 | 여러 개를 가리키면 복수형만 쓴다. `All`을 붙이지 않는다 | `Anjus` (`AllAnju`, `AllAnjus` 금지) |
| Fixture | `*Fixture`는 도메인 객체, Command 같은 데이터 생성을 전담한다 | `MemberFixture` |
| Helper | `*Helper`는 토큰 발급, MockMvc 요청 빌드, 공통 assertion 같은 행동과 절차를 전담한다. 하나의 `*Helper` = 하나의 관심사 | `AuthHelper`, `MockMvcRequestHelper` |

## 구조

- Given-When-Then 블록을 블록 주석으로 구분한다. 주석은 `// given`, `// when`, `// then`, `// when & then` 중에서 쓴다. 준비할 것이 없으면 given을 생략한다. 주석 뒤에 `: 설명`을 붙일 수 있다(예: `// given: 시드 마이그레이션이 적재된 상태`).
- when 블록에는 SUT 호출 하나만 둔다. `@BeforeEach`는 SUT를 실행하지 않고 준비만 한다. given에서 SUT 결과를 고르거나 가공하지 않는다. then 블록에는 검증과 검증에 필요한 상태 재조회만 둔다.
- 블록 주석의 존재와 순서, `should` 접두어는 `.claude/scripts/check-test-conventions.sh`가 검사한다. 블록 안의 내용은 `code-reviewer`가 검토한다.
- 이중 `@Nested` 구조(메서드 레벨 → 조건 레벨)를 쓴다. 조건이 하나뿐이면 조건 레벨을 생략한다.

```java
class MemberServiceTest {

    @Nested
    @DisplayName("회원 조회")
    class GetMember {

        @Nested
        @DisplayName("존재하지 않는 ID인 경우")
        class WhenMemberNotFound {

            @Test
            @DisplayName("예외를 던진다")
            void shouldThrowMemberNotFoundException() {
                // given
                // when
                // then
            }
        }
    }
}
```

## 필드 선언 순서

- 필드는 상수 → `@InjectMocks`(SUT) → `@Mock` 순서다. `@Nested` 안에서도 그 블록의 상수를 다른 필드보다 위에 둔다.
- `@BeforeEach`에서 직접 생성하는 SUT 필드도 `@Mock` 필드보다 먼저 둔다.

```java
class LoginServiceTest extends UnitTest {

    private static final String PROVIDER = "kakao";
    private static final String CODE = "auth-code";

    @InjectMocks private LoginService loginService;

    @Mock private ExchangeSocialCodeUseCase exchangeSocialCodeUseCase;
    @Mock private GetMemberUseCase getMemberUseCase;
}
```

## private 메서드 위치

테스트 클래스의 private 메서드(`given*()` 스텁 추출, 저장 절차 등)는 그 메서드가 속한 클래스의 맨 아래, `@Nested` 클래스와 테스트 메서드 뒤에 둔다. 최상위 클래스에 둘지 쓰는 `@Nested` 안에 둘지는 강제하지 않는다.

## Mockito Strict Stubbing

- `@BeforeEach` 공유 스텁은 모든 테스트가 소비하는 가장 좁은 `@Nested` 스코프에 둔다.
- 같은 스텁 조합이 한 테스트 클래스의 2개 이상 테스트에 그대로 반복되면 `private void given<대상>()` 메서드로 추출한다(예: `givenStandardCatalog()`). 테스트마다 미묘하게 다르면 묶지 않는다.

## 시각 고정

- `Clock` 빈을 주입받는 서비스의 테스트는 `@BeforeEach`에서 `Clock.fixed(NOW.atZone(TimeConfig.SERVICE_ZONE).toInstant(), TimeConfig.SERVICE_ZONE)`로 SUT를 조립한다. 다른 시각이 필요한 테스트만 `givenNow(now)`로 다시 만든다.
- 시각으로 갈리는 규칙은 닫히기 1초 전과 닫히는 순간을 값으로 써서 검증한다. 여유를 둔 값(1시간 전, 49시간 전 등)은 쓰지 않는다.

## Fixture 패턴

- 값이 불안정한 생성(예: 랜덤 UUID)은 도메인별 `*Fixture`로 통일한다. 값 고정형은 enum(예: `MemberFixture`), 값이 달라지는 Command는 Builder로 만든다.
- 테스트 본문에 `X.create(...)`를 반복해서 인라인하지 않는다.
- 도메인별 `fixture` 패키지에 둔다.

## Helper 패턴

- Fixture는 데이터를, Helper는 여러 테스트에서 반복되는 행동과 절차를 맡는다. 둘을 섞지 않는다.
- 도메인별 `helper` 패키지에 둔다. 특정 도메인의 데이터나 빈을 다루지 않는 Helper만 `common/helper`에 둔다(예: `ConcurrencyHelper`).
- 빈을 주입받는 Helper는 `@Component`로, 주입받을 빈이 없는 Helper는 private 생성자를 둔 `final` 클래스의 static 메서드로 둔다.

## 페어링 골든 세트

점수 규칙을 바꿀 때 대표 조합의 순위가 상식에서 벗어나는지 잡는 회귀 테스트다. `PairingQueryServiceTest`의 `Recommend` 안에 둔다.

- 조합: 널리 알려진 음료와 안주 조합만 고르고, 조합 하나가 점수 규칙 하나의 효과를 대표한다. 와인은 X-Wines Harmonize처럼 데이터에 근거가 있는 조합만 기대값으로 둔다.
- 값: 실제 적재 규칙으로 계산한 카탈로그 값을 쓴다. 시드 값과 다른 안주는 `AnjuFixture`의 `CATALOG_` 상수로 둔다.
- 안주 풀: 음료별로 현재 규칙의 카탈로그 상위 안주를 동점까지 모두 넣는다. 기대 성질에 맞지 않는 안주 중 가장 높은 안주와 이전 규칙의 상위 안주를 함정으로 넣고, 함정을 풀 앞에 두어 가장 작은 id를 준다. 음료와 음악은 하나씩만 둔다.
- 검증: 상위 3위 안주가 모두 기대 성질(예: `HARMONIZE_MATCH`)을 갖는지 본다.
- 실패 조합: `@Disabled`에 막는 규칙과 해제할 이슈를 적고, 그 규칙을 구현한 이슈에서 해제한다.
- 점수 규칙이나 적재 규칙을 바꾸면 카탈로그 전체로 순위를 다시 계산해 `CATALOG_` 값과 안주 풀을 갱신한다. 계산용 임시 테스트는 커밋하지 않는다.

## 부하테스트

- 부하테스트 도구는 K6다.
- 범위는 `pairing`의 `GET /api/v1/pairings/recommendations`를 로컬과 dev 프로파일에서 실행하는 것이다. GCP 실행 위치, Grafana Alloy 연동, 인증이 필요한 엔드포인트는 별도 이슈에서 다룬다.
