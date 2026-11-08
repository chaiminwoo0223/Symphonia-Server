# 아키텍처 결정 기록

계층 구조와 크로스 도메인 규칙의 근거, 택하지 않은 안, 변경 이력을 둔다. 현재 규칙은 `architecture` 스킬이 단일 소스다.

## 도메인 수와 shared 패키지

도메인은 총 6개(`member`, `auth`, `catalog`, `pairing`, `gathering`, `notification`)다(#159에서 변경). 3번째 도메인이 늘어난 시점에 `shared` 패키지 도입 여부를 재검토했다. `pairing`은 아직 `member`나 `auth`의 `*UseCase`에 의존하지 않아 별도 `shared` 패키지를 두지 않기로 했다. 크로스 도메인 조율은 대상 도메인의 `*UseCase` 인터페이스를 직접 의존하는 것으로 충분하다. 앞으로 어느 도메인이든(#122에서 변경) 하나의 `*Service`가 크로스 도메인 `*UseCase`를 2개 이상 의존하게 되는 시점에 다시 재검토한다. 4번째 도메인 `gathering`을 추가하면서 이 조건을 다시 확인했다(2026-10-25, 이슈 #122). 현재 설계(#123~#131) 기준으로 `gathering`은 `pairing`의 UseCase로는 `SubmitAnonymousPairingFeedbackUseCase` 하나만(#131에서 변경) 의존하므로 재검토 조건에 해당하지 않는다. #131 구현 기준으로 피드백을 받는 `ParticipantPairingFeedbackService`는 이 UseCase 하나에, 페어링을 조회하는 `ParticipantPairingQueryService`와 페어링을 고르는 `GatheringCommandService`는 `catalog`의 UseCase 하나에 의존한다. 5번째 도메인 `catalog`를 분리하면서 이 조건을 다시 확인했다(2026-10-27, 이슈 #139). #140 설계 기준으로 `PairingQueryService`는 `catalog`의 `GetCatalogUseCase` 하나만, `PairingCommandService`는 `VerifyCatalogReferencesUseCase` 하나만 의존하므로 재검토 조건에 해당하지 않는다. 6번째 도메인 `notification`을 분리하면서 이 조건을 다시 확인했다(2026-11-01, 이슈 #159). `SettlementCommandService`와 `ShareCommandService`가 각각 `notification`의 UseCase 두 개에 의존하지만(#168에서 변경) 더 나누지 않으며, 근거는 크로스 도메인 참조 절의 #159 결정에 있다(이 결정은 `docs/decisions/notification.md`로 옮김, #178에서 변경).

## 도메인 모델과 JPA 엔티티 분리

도메인 모델과 JPA 엔티티는 완전히 분리한다. `domain`은 영속성 기술을 전혀 알지 못한다. Domain↔JPA 변환은 `*JpaEntity`의 정적 팩토리 메서드(`from(도메인객체)`)와 `toDomain()`으로 처리한다. 변환에 암호화 키처럼 다른 값이 필요하면 인자로 받는다(예: `ParticipantJpaEntity.from(participant, gatheringId, dataKey)`). 별도 `*Mapper` 클래스는 두지 않는다(#123에서 변경). 변환 코드가 `*JpaEntity`에 모여 있어야 JPA 엔티티의 필드와 함께 읽히기 때문이다.


## 경로 상수 *Endpoints

`domain`/`application` 계층은 공유 커널인 `common`만 참조하고, 기술 부트스트랩인 `global`(config/security)은 참조하지 않는다. `global`은 오직 Spring 설정·필터 wiring 목적으로만 `common`을 참조할 수 있다.

`global`은 같은 이유로 도메인의 `presentation` 계층에 있는 `*Endpoints`(비즈니스 로직·프레임워크 의존 없이 URL 경로 문자열만 담은 상수 클래스, 예: `auth.presentation.AuthEndpoints`)도 참조할 수 있다. 이는 `SecurityConfig`의 permitAll 매처처럼 순수 wiring 목적에 한한다. `*Request`/`*Response`/`*Api`/`*Controller`처럼 실제 프레젠테이션 로직이 담긴 타입은 여전히 참조 대상이 아니다. URL 경로는 그 경로를 실제로 노출하는 `*Api`와 동기화돼야 하는 도메인 고유의 사실이므로, `global`이 별도 상수로 다시 소유하기보다 도메인 쪽 단일 소스를 참조하는 쪽이 응집도가 높다.

**(2026-09-20 결정, 이슈 #47)**: `*Endpoints`가 경로의 단일 소스이고, `*Api` 자신도 `@GetMapping`/`@PostMapping` 등에 리터럴 경로 문자열 대신 같은 도메인의 `*Endpoints` 상수를 그대로 참조한다(예: `AuthApi`의 `@PostMapping(AuthEndpoints.LOGIN)`, `MemberApi`의 `@GetMapping(MemberEndpoints.ME)`). `*Api`가 리터럴 문자열을 따로 갖고 `*Endpoints`가 그걸 베끼는 구조가 아니다. 클래스 레벨 `@RequestMapping`으로 베이스 경로를 나누지 않고, `*Endpoints`가 `BASE + 세부경로`로 이미 완성해 둔 전체 경로를 각 메서드의 매핑 애노테이션에 직접 넣는다.

## 다른 도메인의 domain 참조

**(2026-10-27 결정, 이슈 #139)**: `src/main` 코드는 다른 도메인의 `domain` 패키지에서 enum만 참조하고, 그 enum이 직접 선언한 메서드는 호출하지 않는다. `name()`, `valueOf()`, `values()` 같은 `java.lang.Enum` 기본 메서드는 도메인 규칙이 없어서 호출할 수 있다. 엔티티와 값 객체는 참조하지 않는다. enum을 값으로 넘기기만 하면 상대 도메인의 규칙에 의존하지 않는다. `auth`가 `member`의 `SocialProvider`를 `*Result`와 외부 연동 응답에 담는 것과, `gathering`이 `pairing`의 `RelationshipType`, `MoodType`을 피드백 Command에 담는 것이 그 예다. enum이 선언한 메서드를 호출하면 상대 도메인의 규칙을 직접 쓰게 된다. 그래서 `pairing`은 `catalog`의 `Harmonize.matches()`를 호출하지 않고, `catalog`가 넘긴 판정 결과를 쓴다. 엔티티와 값 객체까지 막는 이유는 `catalog`와 `pairing`이 같은 이름의 `FlavorProfile`을 각자 가지기 때문이다. 이 규칙이 없으면 잘못 import해도 컴파일과 리뷰를 모두 통과한다. 메서드가 있는 enum의 참조까지 막는 안은 택하지 않았다. 상대 도메인의 UseCase가 받는 enum을 쓸 수 없게 되고, enum의 메서드를 다른 곳으로 옮기면 `switch` 분기만 늘어나기 때문이다. enum을 모두 막는 안도 택하지 않았다. 같은 값 목록을 도메인마다 복제하고 변환 코드만 늘어나기 때문이다. 테스트 코드에는 이 규칙을 적용하지 않는다. 테스트는 다른 도메인의 상태를 준비하거나 검증하려고 그 도메인의 타입을 참조할 수 있다.

## 크로스 도메인 부수효과

크로스 도메인 부수효과(예: 회원 삭제 시 로그아웃 처리)를 어디서 조율할지는 **부수효과 실패가 원래 트랜잭션을 되돌려야 하는지**로 판단한다.

- **Controller에서 각 도메인 `*UseCase`를 직접 순차 호출**: 부수효과가 실패했을 때 원본 작업도 함께 실패(롤백)해야 정합성이 유지되는 경우. 크로스 도메인 `*UseCase` 의존이 1개뿐이고 트리거·부수효과가 단순하면 이 방식으로 충분하며, 굳이 이벤트 인프라를 먼저 두지 않는다.
- **도메인 이벤트(`@TransactionalEventListener(phase = AFTER_COMMIT)`)**: 원본 트랜잭션이 커밋된 뒤에만 의미가 있고, 부수효과가 실패해도 원본 작업을 되돌릴 이유가 없는 후속 정리(cleanup) 성격일 때. 예를 들어 회원 삭제는 그 자체로 완결된 사실이며, 뒤이은 로그아웃 처리(리프레시 토큰 삭제·액세스 토큰 블랙리스트 등록)가 일시적으로 실패하더라도 이미 삭제된 회원을 되살릴 이유는 없다 — 그래서 `MemberDeletedEvent` 기반으로 분리한다.

**이벤트 컨벤션**:
- 네이밍: `<Entity><과거분사>Event` (예: `MemberDeletedEvent`). 순수 도메인 개념(ID 등)뿐 아니라 구독 측이 필요로 하는 요청 컨텍스트(accessToken, ip 등)도 담을 수 있다 — 이벤트는 "구독자에게 필요한 계약"이지, 발행 도메인의 순수 도메인 모델 그 자체는 아니다. 다만 그 컨텍스트가 여러 필드로 늘어나 이벤트의 성격이 모호해지면 그 시점에 페이로드 축소를 재검토한다.
- 정의 위치: 발행하는 도메인의 `application.event` 패키지 (`domain`이 아니다 — `domain`은 프레임워크는 물론, accessToken처럼 다른 도메인/기술 맥락에 속하는 개념도 알지 못해야 한다).
- 발행 위치: 트랜잭션 경계를 가진 `@CommandService` 구현체 내부에서 `ApplicationEventPublisher.publishEvent(...)`로 발행한다 (필드 주입). 트랜잭션 밖(Controller 등)에서 발행하면 `AFTER_COMMIT` 리스너가 걸리지 않는다.
- 구독 위치: 구독하는 도메인의 `application.listener` 패키지에 `@Component` 클래스를 두고 `@TransactionalEventListener(phase = AFTER_COMMIT)` 메서드로 구독한다. 리스너는 대상 도메인의 `*UseCase`를 직접 의존해 위임만 하고, 그 안에 비즈니스 로직을 직접 작성하지 않는다 (`@Component`는 기술적 wiring 전용이라는 기존 원칙과 동일).
- 공유 마커 인터페이스나 베이스 클래스(`common.event` 등)는 두지 않는다. Spring의 `ApplicationEventPublisher`/`@TransactionalEventListener`는 특정 타입을 요구하지 않고, 이벤트가 아직 소수라 공유 추상화를 선제적으로 둘 근거가 없다 (YAGNI). 이벤트 종류가 늘어 정말 공통 로직(로깅, 재시도 등)이 필요해지는 시점에 재검토한다.

## 계층 규칙 검증 도구

Konsist/ArchUnit 같은 아키텍처 자동검증 도구는 도입하지 않기로 했다. 단일 모듈 구조라 패키지 규칙 위반을 코드리뷰(`code-reviewer` 에이전트의 체크리스트 기반 수동 검토)로 잡는 게 더 실용적이라고 판단했기 때문이다. 계층 의존 방향처럼 판단이 섞이는 규칙은 이 방식으로 검증하고(import로 판정할 수 있는 규칙은 `check-architecture.sh`로 옮김, #178에서 변경), EOF 개행·코드 스타일처럼 완전히 결정론적인 것만 스크립트/포맷터로 강제한다.

**(2026-11-08 결정, 이슈 #178)**: import와 선언만 보고 판정할 수 있는 계층 규칙은 `check-architecture.sh`가 grep으로 검사한다. 위반했을 때만 고치는 방법이 컨텍스트에 들어오고, `code-reviewer`는 판단이 필요한 항목만 보게 되어 리뷰 입력이 줄기 때문이다. ArchUnit은 여전히 도입하지 않는다. grep으로 잡히는 규칙에는 테스트 의존성과 테스트 클래스를 추가할 만큼의 이득이 없다. 스크립트의 판정 기준을 스킬에도 규칙으로 올렸다. 다른 도메인에서는 `application`의 `usecase`, `dto`, `event`, `provider`와 `domain`의 enum만 참조한다. event는 도메인 이벤트 구독(#42), provider는 다른 도메인이 선언한 인터페이스 구현(#159)에 필요하다. domain의 프레임워크 import 금지에는 에러 코드 enum의 `HttpStatus`만 예외로 둔다. `ErrorCode` 계약이 `getStatus()`로 `HttpStatus`를 돌려주고, 도메인 에러 코드 enum이 domain에 있기 때문이다.
