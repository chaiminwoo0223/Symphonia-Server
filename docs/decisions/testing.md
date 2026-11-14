# testing 결정 기록

테스트 전략과 컨벤션의 근거, 택하지 않은 안, 변경 이력을 둔다. 현재 규칙은 `testing` 스킬이 단일 소스다. `testing` 스킬에 있던 결정 문단을 원문 그대로 옮겼다(2026-11-08, 이슈 #178).

## catalog를 실제로 조립한 PairingQueryServiceTest

**(2026-10-27 결정, 이슈 #140)**: `PairingQueryServiceTest`는 `GetCatalogUseCase`를 mock하지 않는다. mock Repository를 넣은 실제 `CatalogQueryService`로 `@BeforeEach`에서 SUT를 조립한다. 추천 테스트는 Repository 스텁으로 카탈로그 상태를 준비한다. 이 스텁은 헬퍼 메서드와 연속 반환에 흩어져 있어서 UseCase 스텁으로 옮기면 기존 테스트를 대부분 고쳐야 한다. catalog 분리는 동작을 바꾸지 않는 리팩터링이라 기존 테스트를 그대로 두어 안전망으로 썼다. `CatalogQueryService`는 Repository 결과를 `CatalogResult`로 옮기기만 해서, 함께 실행해도 검증 대상이 흐려지지 않는다. `CatalogQueryService` 자체는 `CatalogQueryServiceTest`가 검증한다.

## 복수 표기

**(2026-09-20 결정, 이슈 #47)**: 여러 개를 가리키는 식별자는 복수형(`Anjus`)만으로 충분하다. `AllAnju`는 단수형에 `All`을 붙여 복수를 흉내 내는 안티패턴이고, `AllAnjus`는 복수형과 `All`이 중복 표현이라 둘 다 쓰지 않는다.

## 예외 테스트 메서드 이름

**(2026-09-19 결정, 이슈 #54)**: 리프 메서드는 조건 레벨 `@Nested` 존재 여부와 무관하게 실제로 던져지는 예외 클래스명을 반영한다(`shouldThrowException()` 금지). `@DisplayName`이나 테스트 리포트만으로는 어떤 예외를 검증하는지 알 수 없어 식별력이 떨어졌기 때문. 예외명이 조건을 자명하게 내포하면 별도로 `When[조건]`을 덧붙이지 않는다. 그러면 동어반복이 되어 "자기 레벨에서만 새 정보를 더한다" 원칙에 반하기 때문.

## given-when-then 검사의 두 층

**(2026-10-17 결정, 이슈 #91)**: 검사를 두 층으로 나눈다. 블록 주석의 존재와 순서, 메서드명 `should` 접두어는 `.claude/scripts/check-test-conventions.sh`가 검사한다. 이 스크립트는 `check-all.sh`에서 빌드 전에 실행된다. 블록 안의 내용이 위 규칙에 맞는지는 판단이 필요해 `code-reviewer`가 검토한다. #76의 `JsonBjcpStyleReaderTest`가 `read()`를 `@BeforeEach`에 숨기고 결과 선택을 given에 둔 채 리뷰를 통과했기 때문이다. `code-style` 스킬의 #77 결정은 Java 구조를 정규식으로 해석하면 오탐이 많다며 awk를 택하지 않았다. 이 스크립트는 블록 주석을 줄 단위 고정 패턴으로 찾고, 메서드 경계는 문자열, 문자, text block, `//` 주석 밖의 중괄호 깊이로만 판정하므로 오탐 범위가 좁아 awk로 둔다. 테스트에서 거의 쓰지 않는 `/* */` 블록 주석은 처리하지 않는다.

## 필드 선언 순서

테스트 클래스 상단 필드는 **상수 → `@InjectMocks` → `@Mock`** 순서로 고정한다. 
상수(`private static final`)를 가장 먼저 두는 건 static이 instance보다 먼저 온다는 일반 Java 컨벤션을 따른 것이고, `@InjectMocks`를 `@Mock`보다 먼저 두는 건 "지금 무엇을 검증하는 테스트인지"(SUT)가 그 협력자들보다 먼저 보여야 읽기 쉽기 때문이다. 
이 순서는 파일 전체뿐 아니라 중첩 스코프에도 그대로 적용한다. 
즉 `@Nested` 안에 그 블록에서만 쓰는 상수를 선언할 때도 그 블록의 다른 필드/설정보다 위에 둔다. 
SUT를 생성자 인자 문제 등으로 Mockito가 `@InjectMocks`로 자동 조립하지 못해 `@BeforeEach`에서 직접 생성하는 경우에도, 이름표만 없을 뿐 그 필드가 SUT라는 사실은 같다. 이때도 그 SUT 필드를 `@Mock` 필드보다 먼저 둔다.

## private 메서드 위치

**(2026-09-27 결정, 이슈 #75)**: 기존 테스트들이 모두 private 메서드를 클래스 맨 아래에 두던 관례를 명시했다. `DrinkRepositoryImplTest`에서 저장 절차 메서드를 필드 바로 아래에 두었다가 관례와 어긋나 옮겼다.

## 반복 스텁 추출

**(2026-09-20 결정, 이슈 #47)**: 동일한 `given(...).willReturn(...)` 스텁 조합이 같은 테스트 클래스 안에서 2개 이상의 테스트 메서드에 그대로 반복되면, `private void given<대상>()` 메서드로 추출해 각 테스트에서 호출한다(예: `givenStandardCatalog()`). `*Fixture`(데이터 생성)나 `*Helper`(여러 파일에서 재사용하는 행동)와는 다른 층위이므로 파일을 분리하지 않고 같은 클래스의 private 메서드로 둔다. 스텁이 테스트마다 미묘하게 달라 추출이 오히려 읽기 어려워지면 억지로 묶지 않는다.

## 시각 고정

**(2026-11-01 결정, 이슈 #158)**: `Clock` 빈을 주입받는 서비스의 테스트는 실제 시계를 쓰지 않는다. `@BeforeEach`에서 `Clock.fixed(NOW.atZone(TimeConfig.SERVICE_ZONE).toInstant(), TimeConfig.SERVICE_ZONE)`로 SUT를 조립한다. 다른 시각이 필요한 테스트만 `givenNow(now)`로 그 시각에 고정한 SUT를 다시 만든다(예: `PrepareNotificationServiceTest`). 48시간 기록 기간처럼 시각으로 갈리는 규칙은 닫히기 1초 전과 닫히는 순간을 값으로 써서 검증한다. 실제 시계 기준으로 1시간 전, 49시간 전처럼 여유를 둔 값은 경계 판정을 `isBefore`에서 `!isAfter`로 바꿔도 통과하기 때문이다. #158에서 정책을 그렇게 바꿔 보니 경계값으로 바꾼 테스트 7개가 모두 실패했다.

## 페어링 골든 세트

**(2026-10-20 결정, 이슈 #105)**: 안주 풀에 카탈로그 상위 안주를 넣은 이유는, 풀이 작으면 카탈로그에서는 실패하는 조합이 테스트에서 통과하기 때문이다. 처음 시도한 5개 풀에서는 카탈로그 24위인 레드 와인과 치즈플래터가 3위로 통과했다. 시드 값 픽스처는 실제 카탈로그와 맛이 달라 실제 추천 결과를 대표하지 못해 쓰지 않았다. 상위 5위는 추천 결과 전체라 검증하는 의미가 없어 3위로 잡았다. 처음에는 안주 하나(소주와 삼겹살구이)가 3위 안에 드는지 검증했다. 그런데 삼겹살구이, 대창구이, 소막창구이는 맛 값이 같아 어떤 규칙에서도 동점이고 순서가 id로만 갈려서, 상위 3위의 성질을 검증하는 방식으로 바꿨다. 레드 와인과 치즈플래터도 Louis Jadot Beaujolais의 X-Wines Harmonize(Pasta, Veal, Pork, Poultry)에 치즈가 없어 기대값에서 뺐다. 기대 성질에 맞지 않는 안주를 풀에 넣는 이유는 규칙을 되돌리면 테스트가 실패하게 하기 위해서다. 함정 안주에 가장 작은 id를 주는 이유는, 고다치즈처럼 기대 안주와 동점인 함정이 id 순서로 밀려나면 규칙을 되돌려도 테스트가 통과하기 때문이다. 실제로 함정을 풀 뒤쪽에 두었을 때는 Harmonize 점수를 0으로 바꿔도 소주 골든 테스트가 통과했다. 함정을 앞으로 옮긴 뒤에는 Harmonize 점수나 도수 대비 점수를 0으로 바꾸면 소주 골든 테스트가, Harmonize 점수를 0으로 바꾸면 레드 와인 골든 테스트가 실패하는 것을 확인했다.

## ConcurrencyHelper 위치

**(2026-10-31 결정, 이슈 #124)**: 두 Repository 테스트에 같은 동시 실행 메서드(`runConcurrently`, `await`)가 복사되어 있어 `gathering/helper/ConcurrencyHelper`로 옮겼다. 기존 Helper는 Repository를 주입받는 `@Component`라 `@SpringBootTest`에서만 쓴다. `ConcurrencyHelper`는 주입받을 빈이 없고 `@DataJpaTest` 기반 `RepositoryTest`에서 쓰므로, `@Component`로 두면 빈이 필요 없는 클래스를 쓰려고 테스트마다 `@Import`를 붙여야 한다. 그래서 static 메서드로 두었다. 동시 실행은 도메인과 무관하지만 지금 쓰는 곳이 gathering뿐이라 도메인별 배치 규칙을 따랐다.

**(2026-11-07 결정, 이슈 #130)**: `ConcurrencyHelper`를 `common/helper`로 옮겼다. notification(#159)과 pairing(#130)의 Repository 테스트도 쓰게 되어, gathering에 두면 다른 도메인 테스트가 gathering 테스트 패키지를 참조한다. 특정 도메인의 데이터나 빈을 다루지 않는 Helper만 `common/helper`에 두고, 도메인 데이터를 다루는 Helper는 계속 도메인별 `helper` 패키지에 둔다.

## 부하테스트 도구

**(2026-09-24 결정, 이슈 #70)**: 부하테스트 도구로 K6를 채택한다. GCP와 Grafana Cloud/Alloy 스택을 쓰기로 했는데, K6는 Grafana Labs 소유 프로젝트라 Prometheus remote write 출력이 Alloy·Grafana Cloud와 별도 브릿지 없이 연동된다. 단일 바이너리/컨테이너라 GCP에서 상시 인프라 없이 실행되고, Go 기반이라 JVM 스레드 기반인 nGrinder·JMeter보다 동일 자원으로 더 큰 동시 사용자를 시뮬레이션한다. nGrinder는 Controller/Agent 상시 인프라가 필요하고 Grafana 네이티브 연동이 없어서 제외했다. JMeter는 XML 기반 GUI 워크플로라 코드 리뷰·CI 게이트 친화성이 떨어져서 제외했다. JDBC·JMS 등 비HTTP 프로토콜까지 부하테스트 범위가 넓어지면 JMeter 도입을 재검토한다.

1차 부하테스트는 `pairing` 도메인의 `GET /api/v1/pairings/recommendations`를 대상으로 로컬과 dev 프로파일 환경에서 K6 스크립트를 실행하는 범위로 시작한다(이슈 #70). GCP 실행 위치와 Grafana Alloy 연동은 인프라 협의가 끝난 뒤 별도 이슈에서 다룬다. 인증이 필요한 `FEEDBACK` 엔드포인트를 포함한 로그인 플로우도 범위를 넓힐 때 별도 이슈에서 다룬다.

## 스트레스·스파이크 테스트 시점

**(2026-11-14 결정, 이슈 #180)**: 스트레스와 스파이크 테스트는 dev 배포 이후로 미루고, 스크립트도 그때 추가한다. 로컬에서는 앱, Postgres, Redis, K6가 한 머신의 CPU를 나눠 쓰므로 측정한 한계 값이 서버 사양이 아니라 개발 머신의 한계가 된다. 무너진 원인을 보려면 CPU, 커넥션 풀, GC 지표가 필요한데 actuator 연동(#174)이 아직 없다. 지금 도입하는 안은 이 두 이유로 택하지 않았다. 도입 필요성은 있다. 술자리 서비스라 트래픽이 평일 저녁에 몰리고 정산 알림이 한꺼번에 나가는 시점이 있어 스파이크가 의미 있고, 커넥션 풀과 Redis 타임아웃 같은 설정값의 한계를 숫자로 알려면 스트레스가 필요하다. prod에는 실행하지 않는다. dev 배포 일정과 dev 사양이 정해지면 시나리오와 스크립트를 다루는 이슈를 만든다.

## 테스트 컨테이너 공유

**(2026-11-08 결정, 이슈 #178)**: Postgres와 Redis 컨테이너를 `TestcontainersConfiguration`의 static 필드로 테스트 JVM에서 한 번만 띄운다. 컨테이너를 `@Bean`으로 선언하면 Repository 테스트마다 다른 `@Import` 조합 때문에 생기는 스프링 컨텍스트 약 25개가 각자 컨테이너 3개를 띄우고 Flyway를 처음부터 실행했다. `check-all.sh`는 198초에서 65초로 줄었다. `@ImportTestcontainers`로 static 필드를 등록하는 안은 택하지 않았다. 컨테이너가 빈으로 등록되어, 컨텍스트 캐시가 오래된 컨텍스트를 닫을 때 공유 컨테이너가 멈추고 새 포트로 다시 떠 캐시에 남은 컨텍스트가 연결에 실패했다. 그래서 컨테이너는 빈으로 두지 않고 `DynamicPropertyRegistrar`로 접속 정보만 넘긴다. RabbitMQ는 공유하지 않는다. 캐시된 다른 `@SpringBootTest` 컨텍스트의 리스너가 `NotificationDeliveryListenerTest`가 발행한 메시지를 가져갈 수 있기 때문이다. 대신 `RabbitTestcontainersConfiguration`으로 나눠 큐 리스너가 뜨는 `IntegrationTest` 컨텍스트에서만 띄운다. 공유 Postgres에 컨텍스트마다 기본 10개인 커넥션 풀이 붙어 `max_connections` 100을 넘었다(`too many clients already`). 그래서 테스트 커넥션 풀을 4개로 줄이고, 컨텍스트 캐시 상한 32개가 모두 차도 넘지 않게 `max_connections`를 200으로 늘렸다. 테스트 병렬 fork는 공유 DB 경합으로 결과가 흔들릴 수 있어 택하지 않았다. Testcontainers reuse는 실행 사이에 데이터가 남고 홈 디렉터리 설정에 의존해 택하지 않았다.
