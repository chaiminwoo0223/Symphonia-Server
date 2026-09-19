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

## 명명 규칙

각 이름은 **자기 레벨에서만 새로운 정보를 더한다.** 상위 레벨(클래스명·`@DisplayName`)이 이미 표현한 내용을 하위 레벨(메서드명)에서 다시 쓰지 않는다. 사람이 읽을 설명은 전부 `@DisplayName`(한국어)에 담고, 식별자(클래스명·메서드명)에는 한국어를 쓰지 않는다.

| 대상 | 규칙 | 예 |
|---|---|---|
| 테스트 클래스 | `*Test` (통합 테스트는 `*IntegrationTest`) | `MemberServiceTest` |
| 메서드 레벨 `@Nested` | 계층 무관, **테스트 대상 동작**(메서드/쿼리/엔드포인트)을 나타내는 PascalCase 명사. 접두어 없음 | Service: `GetMember` · Repository: `FindByEmail` · Controller: `CreateMember` |
| 조건 레벨 `@Nested` | `When` + 조건(PascalCase). **조건이 여러 갈래로 나뉠 때만** 사용 | `WhenMemberNotFound` |
| 테스트 메서드(리프) | `should` + 기대동작. **예외/실패 케이스는 조건 레벨 `@Nested`가 있어도 검증하는 구체적 예외 타입명을 그대로 담는다**(`assertThatThrownBy(...).isInstanceOf(...)` 대상). 조건 레벨 없이 단일 케이스만 테스트한다면 `should[기대동작]When[조건]`을 그대로 쓰되, 예외명 자체가 조건을 이미 드러내면(`MemberNotFoundException`처럼) `When[조건]`을 반복해 붙이지 않는다 | 조건 있음: `shouldThrowMemberNotFoundException()` · 성공 케이스: `shouldReturnMemberResultWhenMemberExists()` |
| Fixture | `*Fixture`는 도메인 객체/Command 등 **데이터** 생성을 전담한다 | `MemberFixture` |
| Helper | `*Helper`는 인증 토큰 발급, MockMvc 요청 빌드, 공통 assertion 등 **행동/절차**를 전담한다. 하나의 `*Helper` = 하나의 관심사 (범용 유틸리티 클래스 금지) | `AuthHelper`, `MockMvcRequestHelper` |

**(2026-09-19 결정, 이슈 #54)**: 리프 메서드는 조건 레벨 `@Nested` 존재 여부와 무관하게 실제로 던져지는 예외 클래스명을 반영한다(`shouldThrowException()` 금지). `@DisplayName`이나 테스트 리포트만으로는 어떤 예외를 검증하는지 알 수 없어 식별력이 떨어졌기 때문. 예외명이 조건을 자명하게 내포하면 별도로 `When[조건]`을 덧붙이지 않는다. 그러면 동어반복이 되어 "자기 레벨에서만 새 정보를 더한다" 원칙에 반하기 때문.

## 구조

- **Given-When-Then 패턴을 고정**한다. 세 블록을 주석이나 공백으로 명확히 구분한다.
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

## Mockito Strict Stubbing

- `@BeforeEach` 공유 스텁은 **모든 테스트가 소비하는 가장 좁은 `@Nested` 스코프**에 배치한다. 상위 스코프에 두면 일부 테스트에서 미사용 스텁으로 strict-stubbing 검증에 걸린다.
- **(2026-09-20 결정, 이슈 #47)**: 동일한 `given(...).willReturn(...)` 스텁 조합이 같은 테스트 클래스 안에서 2개 이상의 테스트 메서드에 그대로 반복되면, `private void given<대상>()` 메서드로 추출해 각 테스트에서 호출한다(예: `givenStandardCatalog()`). `*Fixture`(데이터 생성)나 `*Helper`(여러 파일에서 재사용하는 행동)와는 다른 층위이므로 파일을 분리하지 않고 같은 클래스의 private 메서드로 둔다. 스텁이 테스트마다 미묘하게 달라 추출이 오히려 읽기 어려워지면 억지로 묶지 않는다.

## Fixture 패턴

값이 불안정하면(예: 랜덤 UUID) 테스트 어설션이 흔들리므로, 생성 로직을 도메인별 `*Fixture`로 통일한다.

- **값 고정형**은 enum으로 관리 (예: `MemberFixture`).
- **값이 달라지는 Command**는 Builder 패턴으로 생성한다.
- 테스트 본문에 `X.create(...)`를 반복해서 인라인하지 않고 `*Fixture`를 통해 구성한다.
- 도메인별 `fixture` 패키지(각 모듈의 `src/test`)에 둔다.

## Helper 패턴

Fixture가 "데이터"를 만든다면, Helper는 여러 테스트에서 반복되는 "행동·절차"를 캡슐화한다. 이 둘을 섞으면(예: Fixture가 HTTP 요청까지 수행) 책임이 흐려지므로 분리한다.

- 예: 인증이 필요한 통합 테스트의 토큰 발급/헤더 세팅(`AuthHelper`), 반복되는 MockMvc 요청 빌더(`MockMvcRequestHelper`), 공통 응답 검증(`ResponseAssertHelper`).
- **`Helper` 접미사가 "만능 유틸리티 클래스"의 핑계가 되지 않게 한다.** 클린코드에서 `Util`/`Manager`처럼 책임이 불분명한 이름을 지양하는 것과 같은 이유로, 하나의 `*Helper`는 하나의 관심사만 다룬다. 인증 Helper와 요청 빌드 Helper를 하나로 합치지 않는다.
- `*Fixture`와 마찬가지로 도메인별 `helper` 패키지(각 모듈의 `src/test`)에 둔다.
