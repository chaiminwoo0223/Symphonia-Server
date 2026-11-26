# 도메인 이벤트와 전환 지표

이 문서는 현재 규칙만 담는다. 근거, 택하지 않은 안, 변경 이력은 `docs/decisions/architecture.md`에 있다.

## 크로스 도메인 부수효과

- 부수효과가 실패했을 때 원본 작업도 함께 실패해야 하면 원본 도메인의 `@CommandService` 안에서 대상 도메인의 `*UseCase`를 부른다. 두 작업이 한 트랜잭션에서 함께 커밋되거나 함께 롤백된다. Controller에서 UseCase를 차례로 부르면 앞 UseCase가 먼저 커밋되므로 이렇게 조율하지 않는다.
- 원본이 커밋된 뒤에만 의미가 있고 실패해도 원본을 되돌릴 이유가 없는 후속 정리라면 `@TransactionalEventListener(phase = AFTER_COMMIT)` 도메인 이벤트로 분리한다(예: 회원 삭제 뒤 로그아웃 처리).
- 원본과 같은 DB를 바꾸고 원본과 함께 롤백돼야 하는 후속 처리는 `@TransactionalEventListener(phase = BEFORE_COMMIT)` 리스너로 한다(예: 회원 삭제 때 `pairing_feedback` 익명화). 원본이 대상 도메인을 이미 의존하면 위처럼 UseCase를 부르고, 대상이 원본을 의존하는 방향이면 원본이 대상을 의존하지 않도록 이 리스너를 쓴다. 커밋 뒤 리스너의 DB 쓰기는 이미 끝난 트랜잭션에 참여해 반영되지 않는다.
- `AFTER_COMMIT` 리스너가 실패해도 API 응답은 바뀌지 않는다. Spring이 ERROR 로그만 남기고 응답은 원본 그대로다.
- 이벤트 이름은 `<Entity><과거분사>Event`(예: `MemberDeletedEvent`)다. 구독자에게 필요한 요청 컨텍스트(accessToken, ip 등)도 담을 수 있다.
- 이벤트는 발행하는 도메인의 `application.event`에 정의하고, `@CommandService` 구현체 안에서 `ApplicationEventPublisher.publishEvent(...)`로 발행한다. 전환 지표 이벤트는 `@QueryService`에서도 발행한다. 트랜잭션 밖에서 발행하면 `AFTER_COMMIT` 리스너가 걸리지 않는다.
- 구독은 구독하는 도메인의 `application.listener`에 `@Component` 클래스로 두고, 대상 도메인의 `*UseCase`에 위임만 한다. 전환 지표 리스너(`*MetricListener`)는 `*UseCase`가 아니라 같은 도메인의 `application.recorder` 인터페이스에 위임한다.
- 공유 마커 인터페이스나 베이스 클래스(`common.event` 등)는 두지 않는다.

## 전환 지표

- 전환 지표는 Micrometer 카운터로 건수만 센다. 라벨에 개인정보와 식별자를 넣지 않고, 경로는 `type` 라벨로 구분한다.
- 기록 인터페이스는 `application.recorder`의 `*MetricRecorder`, 구현체는 `infrastructure.metric`의 `Micrometer*MetricRecorder`다. 카운터는 시작할 때 모두 등록한다.
- `AFTER_COMMIT` 리스너에서 센다.
- 새 결과가 생긴 경우에만 이벤트를 발행한다. 같은 요청 id의 모임 열기와 정산 건 기록, 바뀐 분담금이 없는 "보냈어요", 회원의 피드백 재제출은 발행하지 않는다. "보냈어요"는 요청마다 한 번 센다. 추천은 같은 요청이어도 성공할 때마다 센다.
