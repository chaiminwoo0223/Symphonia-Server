---
name: testing
description: Load when writing or modifying tests. JUnit5/Mockito/Testcontainers, given-when-then 패턴, 계층별 테스트 전략, 이중 @Nested 구조, 명명 규칙, Fixture/Helper 패턴.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Test Rules

## 테스트 라이브러리

| 목적 | 라이브러리 |
|---|---|
| 테스트 프레임워크 | JUnit 5 |
| 모킹 | Mockito |
| 통합 테스트 인프라 | Testcontainers |

## 계층별 테스트 범위

| 계층 | 검증 대상 | 베이스 클래스 |
|---|---|---|
| `infrastructure` (JPA Repository) | JPA 쿼리 반환값 정확성 | `RepositoryTest` (`@DataJpaTest` + Testcontainers) |
| `infrastructure` (Redis Repository) | Redis 저장/조회 정확성 | `RedisRepositoryTest` (`@DataRedisTest` + Testcontainers) |
| `application` (Service) | 비즈니스 로직 수행 여부 | `UnitTest` (`MockitoExtension`) |
| `presentation` (Controller) | 요청/응답, 인증/인가, 예외 매핑 | `IntegrationTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`) |

- **`domain` 계층 테스트는 만들지 않는다.** 도메인 로직은 그 로직을 쓰는 `*Service` 테스트로 검증한다(예: `Pairing.recommend`는 `PairingQueryServiceTest`). 최적화 검증처럼 일시적으로 도메인 테스트가 필요하면 커밋하지 않고 작업이 끝나면 삭제한다.

**(2026-10-27 결정, 이슈 #140)**: `PairingQueryServiceTest`는 `GetCatalogUseCase`를 mock하지 않는다. mock Repository를 넣은 실제 `CatalogQueryService`로 `@BeforeEach`에서 SUT를 조립한다. 추천 테스트는 Repository 스텁으로 카탈로그 상태를 준비한다. 이 스텁은 헬퍼 메서드와 연속 반환에 흩어져 있어서 UseCase 스텁으로 옮기면 기존 테스트를 대부분 고쳐야 한다. catalog 분리는 동작을 바꾸지 않는 리팩터링이라 기존 테스트를 그대로 두어 안전망으로 썼다. `CatalogQueryService`는 Repository 결과를 `CatalogResult`로 옮기기만 해서, 함께 실행해도 검증 대상이 흐려지지 않는다. `CatalogQueryService` 자체는 `CatalogQueryServiceTest`가 검증한다.

## 명명 규칙

각 이름은 **자기 레벨에서만 새로운 정보를 더한다.** 상위 레벨(클래스명·`@DisplayName`)이 이미 표현한 내용을 하위 레벨(메서드명)에서 다시 쓰지 않는다. 사람이 읽을 설명은 전부 `@DisplayName`(한국어)에 담고, 식별자(클래스명·메서드명)에는 한국어를 쓰지 않는다.

| 대상 | 규칙 | 예 |
|---|---|---|
| 테스트 클래스 | `*Test` (통합 테스트는 `*IntegrationTest`) | `MemberServiceTest` |
| 메서드 레벨 `@Nested` | 계층 무관, **테스트 대상 동작**(메서드/쿼리/엔드포인트)을 나타내는 PascalCase 명사. 접두어 없음 | Service: `GetMember` · Repository: `FindByEmail` · Controller: `CreateMember` |
| 조건 레벨 `@Nested` | `When` + 조건(PascalCase). **조건이 여러 갈래로 나뉠 때만** 사용 | `WhenMemberNotFound` |
| 테스트 메서드(리프) | `should` + 기대동작. **예외/실패 케이스는 조건 레벨 `@Nested`가 있어도 검증하는 구체적 예외 타입명을 그대로 담는다**(`assertThatThrownBy(...).isInstanceOf(...)` 대상). 조건 레벨 없이 단일 케이스만 테스트한다면 `should[기대동작]When[조건]`을 그대로 쓰되, 예외명 자체가 조건을 이미 드러내면(`MemberNotFoundException`처럼) `When[조건]`을 반복해 붙이지 않는다 | 조건 있음: `shouldThrowMemberNotFoundException()` · 성공 케이스: `shouldReturnMemberResultWhenMemberExists()` |
| 복수 표기 | 식별자(메서드명 등)에서 여러 개를 가리킬 때는 대상 이름의 복수형만 쓴다. `All` 같은 수식어를 앞에 붙이지 않는다 | `shouldReturnEmptyListWhenAnjusExcludedByAllergies()` (`AllAnju`, `AllAnjus` 금지) |
| Fixture | `*Fixture`는 도메인 객체/Command 등 **데이터** 생성을 전담한다 | `MemberFixture` |
| Helper | `*Helper`는 인증 토큰 발급, MockMvc 요청 빌드, 공통 assertion 등 **행동/절차**를 전담한다. 하나의 `*Helper` = 하나의 관심사 (범용 유틸리티 클래스 금지) | `AuthHelper`, `MockMvcRequestHelper` |

**(2026-09-20 결정, 이슈 #47)**: 여러 개를 가리키는 식별자는 복수형(`Anjus`)만으로 충분하다. `AllAnju`는 단수형에 `All`을 붙여 복수를 흉내 내는 안티패턴이고, `AllAnjus`는 복수형과 `All`이 중복 표현이라 둘 다 쓰지 않는다.

**(2026-09-19 결정, 이슈 #54)**: 리프 메서드는 조건 레벨 `@Nested` 존재 여부와 무관하게 실제로 던져지는 예외 클래스명을 반영한다(`shouldThrowException()` 금지). `@DisplayName`이나 테스트 리포트만으로는 어떤 예외를 검증하는지 알 수 없어 식별력이 떨어졌기 때문. 예외명이 조건을 자명하게 내포하면 별도로 `When[조건]`을 덧붙이지 않는다. 그러면 동어반복이 되어 "자기 레벨에서만 새 정보를 더한다" 원칙에 반하기 때문.

## 구조

- **Given-When-Then 패턴을 고정**한다. 세 블록을 블록 주석으로 명확히 구분한다. 공백만으로 구분하면 `check-test-conventions.sh`에 걸린다.
  - 블록 주석은 `// given`, `// when`, `// then`, `// when & then` 중에서 쓴다. given은 준비할 것이 없으면 생략한다. 주석 뒤에 `: 설명`을 붙일 수 있다(예: `// given: 시드 마이그레이션이 적재된 상태`).
  - when 블록에는 SUT 호출 하나만 둔다. `@BeforeEach`에서는 SUT를 실행하지 않고 준비만 한다. given에서 SUT 결과를 고르거나 가공하지 않는다. then 블록에는 검증과 검증에 필요한 상태 재조회만 둔다.
  - **(2026-10-17 결정, 이슈 #91)**: 검사를 두 층으로 나눈다. 블록 주석의 존재와 순서, 메서드명 `should` 접두어는 `.claude/scripts/check-test-conventions.sh`가 검사한다. 이 스크립트는 `check-all.sh`에서 빌드 전에 실행된다. 블록 안의 내용이 위 규칙에 맞는지는 판단이 필요해 `code-reviewer`가 검토한다. #76의 `JsonBjcpStyleReaderTest`가 `read()`를 `@BeforeEach`에 숨기고 결과 선택을 given에 둔 채 리뷰를 통과했기 때문이다. `code-style` 스킬의 #77 결정은 Java 구조를 정규식으로 해석하면 오탐이 많다며 awk를 택하지 않았다. 이 스크립트는 블록 주석을 줄 단위 고정 패턴으로 찾고, 메서드 경계는 문자열, 문자, text block, `//` 주석 밖의 중괄호 깊이로만 판정하므로 오탐 범위가 좁아 awk로 둔다. 테스트에서 거의 쓰지 않는 `/* */` 블록 주석은 처리하지 않는다.
- **이중 `@Nested` 구조**: 메서드 레벨 → 조건 레벨. 조건이 하나뿐이면 조건 레벨은 생략할 수 있다 (억지로 중첩시키지 않는다).

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

테스트 클래스 상단 필드는 **상수 → `@InjectMocks` → `@Mock`** 순서로 고정한다. 
상수(`private static final`)를 가장 먼저 두는 건 static이 instance보다 먼저 온다는 일반 Java 컨벤션을 따른 것이고, `@InjectMocks`를 `@Mock`보다 먼저 두는 건 "지금 무엇을 검증하는 테스트인지"(SUT)가 그 협력자들보다 먼저 보여야 읽기 쉽기 때문이다. 
이 순서는 파일 전체뿐 아니라 중첩 스코프에도 그대로 적용한다. 
즉 `@Nested` 안에 그 블록에서만 쓰는 상수를 선언할 때도 그 블록의 다른 필드/설정보다 위에 둔다. 
SUT를 생성자 인자 문제 등으로 Mockito가 `@InjectMocks`로 자동 조립하지 못해 `@BeforeEach`에서 직접 생성하는 경우에도, 이름표만 없을 뿐 그 필드가 SUT라는 사실은 같다. 이때도 그 SUT 필드를 `@Mock` 필드보다 먼저 둔다.

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

테스트 클래스 안의 private 메서드(`given*()` 스텁 추출, 저장 절차 추출 등)는 그 메서드가 속한 클래스의 맨 아래, 즉 `@Nested` 클래스와 테스트 메서드보다 뒤에 둔다. 필드 순서 규칙과 같은 이유로, 파일을 열었을 때 무엇을 검증하는지가 준비 절차보다 먼저 보여야 읽기 쉽다. 최상위 클래스에 둘지 그 메서드를 쓰는 `@Nested` 안에 둘지는 강제하지 않는다(예: 최상위 맨 아래에 둔 `PairingControllerTest`의 `seedCatalog()`, `Recommend` 안 맨 아래에 둔 `PairingQueryServiceTest`의 `givenStandardCatalog()`).

**(2026-09-27 결정, 이슈 #75)**: 기존 테스트들이 모두 private 메서드를 클래스 맨 아래에 두던 관례를 명시했다. `DrinkRepositoryImplTest`에서 저장 절차 메서드를 필드 바로 아래에 두었다가 관례와 어긋나 옮겼다.

## Mockito Strict Stubbing

- `@BeforeEach` 공유 스텁은 **모든 테스트가 소비하는 가장 좁은 `@Nested` 스코프**에 배치한다. 상위 스코프에 두면 일부 테스트에서 미사용 스텁으로 strict-stubbing 검증에 걸린다.
- **(2026-09-20 결정, 이슈 #47)**: 동일한 `given(...).willReturn(...)` 스텁 조합이 같은 테스트 클래스 안에서 2개 이상의 테스트 메서드에 그대로 반복되면, `private void given<대상>()` 메서드로 추출해 각 테스트에서 호출한다(예: `givenStandardCatalog()`). `*Fixture`(데이터 생성)나 `*Helper`(여러 파일에서 재사용하는 행동)와는 다른 층위이므로 파일을 분리하지 않고 같은 클래스의 private 메서드로 둔다. 스텁이 테스트마다 미묘하게 달라 추출이 오히려 읽기 어려워지면 억지로 묶지 않는다.

## Fixture 패턴

값이 불안정하면(예: 랜덤 UUID) 테스트 어설션이 흔들리므로, 생성 로직을 도메인별 `*Fixture`로 통일한다.

- **값 고정형**은 enum으로 관리 (예: `MemberFixture`).
- **값이 달라지는 Command**는 Builder 패턴으로 생성한다.
- 테스트 본문에 `X.create(...)`를 반복해서 인라인하지 않고 `*Fixture`를 통해 구성한다.
- 도메인별 `fixture` 패키지(각 모듈의 `src/test`)에 둔다.

## 페어링 골든 세트

점수 규칙을 바꿀 때 대표 조합의 순위가 상식에서 벗어나는지 잡는 회귀 테스트다. `PairingQueryServiceTest`의 `Recommend` 안에 둔다.

- **조합**: 널리 알려진 음료와 안주 조합만 고른다. 조합 하나는 점수 규칙 하나(도수 대비, Harmonize 등)의 효과를 대표한다. 와인은 X-Wines Harmonize처럼 데이터에 근거가 있는 조합만 기대값으로 둔다.
- **값**: 음료와 안주는 실제 적재 규칙으로 계산한 카탈로그 값을 쓴다. 시드 값과 다른 안주는 `AnjuFixture`의 `CATALOG_` 상수로 따로 둔다.
- **안주 풀**: 골든 세트 음료별로 현재 규칙의 카탈로그 상위 안주를 동점까지 모두 넣는다. 기대 성질에 맞지 않는 안주 중 가장 높은 안주와, 이전 규칙의 상위 안주도 함정으로 넣는다. 함정 안주를 풀 앞에 두고 순서대로 id를 매겨, 동점이면 함정이 먼저 오는 가장 불리한 경우를 검증한다. 음료와 음악은 하나씩만 두어 결과 순위가 곧 그 음료의 안주 순위가 되게 한다.
- **검증**: 안주 하나가 아니라 상위 3위 안주가 모두 기대 성질(예: `HARMONIZE_MATCH` 추천 이유)을 갖는지 본다.
- **실패 조합**: 현재 규칙에서 실패하는 조합은 `@Disabled`에 막는 규칙과 해제할 이슈를 적는다. 그 규칙을 구현한 이슈에서 해제한다.
- **규칙 변경 시**: 점수 규칙이나 적재 규칙을 바꾸면 카탈로그 전체로 순위를 다시 계산해 `CATALOG_` 값과 안주 풀을 갱신한다. 계산용 임시 테스트는 커밋하지 않는다.

**(2026-10-20 결정, 이슈 #105)**: 안주 풀에 카탈로그 상위 안주를 넣은 이유는, 풀이 작으면 카탈로그에서는 실패하는 조합이 테스트에서 통과하기 때문이다. 처음 시도한 5개 풀에서는 카탈로그 24위인 레드 와인과 치즈플래터가 3위로 통과했다. 시드 값 픽스처는 실제 카탈로그와 맛이 달라 실제 추천 결과를 대표하지 못해 쓰지 않았다. 상위 5위는 추천 결과 전체라 검증하는 의미가 없어 3위로 잡았다. 처음에는 안주 하나(소주와 삼겹살구이)가 3위 안에 드는지 검증했다. 그런데 삼겹살구이, 대창구이, 소막창구이는 맛 값이 같아 어떤 규칙에서도 동점이고 순서가 id로만 갈려서, 상위 3위의 성질을 검증하는 방식으로 바꿨다. 레드 와인과 치즈플래터도 Louis Jadot Beaujolais의 X-Wines Harmonize(Pasta, Veal, Pork, Poultry)에 치즈가 없어 기대값에서 뺐다. 기대 성질에 맞지 않는 안주를 풀에 넣는 이유는 규칙을 되돌리면 테스트가 실패하게 하기 위해서다. 함정 안주에 가장 작은 id를 주는 이유는, 고다치즈처럼 기대 안주와 동점인 함정이 id 순서로 밀려나면 규칙을 되돌려도 테스트가 통과하기 때문이다. 실제로 함정을 풀 뒤쪽에 두었을 때는 Harmonize 점수를 0으로 바꿔도 소주 골든 테스트가 통과했다. 함정을 앞으로 옮긴 뒤에는 Harmonize 점수나 도수 대비 점수를 0으로 바꾸면 소주 골든 테스트가, Harmonize 점수를 0으로 바꾸면 레드 와인 골든 테스트가 실패하는 것을 확인했다.

## Helper 패턴

Fixture가 "데이터"를 만든다면, Helper는 여러 테스트에서 반복되는 "행동·절차"를 캡슐화한다. 이 둘을 섞으면(예: Fixture가 HTTP 요청까지 수행) 책임이 흐려지므로 분리한다.

- 예: 인증이 필요한 통합 테스트의 토큰 발급/헤더 세팅(`AuthHelper`), 반복되는 MockMvc 요청 빌더(`MockMvcRequestHelper`), 공통 응답 검증(`ResponseAssertHelper`).
- **`Helper` 접미사가 "만능 유틸리티 클래스"의 핑계가 되지 않게 한다.** 클린코드에서 `Util`/`Manager`처럼 책임이 불분명한 이름을 지양하는 것과 같은 이유로, 하나의 `*Helper`는 하나의 관심사만 다룬다. 인증 Helper와 요청 빌드 Helper를 하나로 합치지 않는다.
- `*Fixture`와 마찬가지로 도메인별 `helper` 패키지(각 모듈의 `src/test`)에 둔다.

## 부하테스트

**(2026-09-24 결정, 이슈 #70)**: 부하테스트 도구로 K6를 채택한다. GCP와 Grafana Cloud/Alloy 스택을 쓰기로 했는데, K6는 Grafana Labs 소유 프로젝트라 Prometheus remote write 출력이 Alloy·Grafana Cloud와 별도 브릿지 없이 연동된다. 단일 바이너리/컨테이너라 GCP에서 상시 인프라 없이 실행되고, Go 기반이라 JVM 스레드 기반인 nGrinder·JMeter보다 동일 자원으로 더 큰 동시 사용자를 시뮬레이션한다. nGrinder는 Controller/Agent 상시 인프라가 필요하고 Grafana 네이티브 연동이 없어서 제외했다. JMeter는 XML 기반 GUI 워크플로라 코드 리뷰·CI 게이트 친화성이 떨어져서 제외했다. JDBC·JMS 등 비HTTP 프로토콜까지 부하테스트 범위가 넓어지면 JMeter 도입을 재검토한다.

1차 부하테스트는 `pairing` 도메인의 `GET /api/v1/pairings/recommendations`를 대상으로 로컬과 dev 프로파일 환경에서 K6 스크립트를 실행하는 범위로 시작한다(이슈 #70). GCP 실행 위치와 Grafana Alloy 연동은 인프라 협의가 끝난 뒤 별도 이슈에서 다룬다. 인증이 필요한 `FEEDBACK` 엔드포인트를 포함한 로그인 플로우도 범위를 넓힐 때 별도 이슈에서 다룬다.
