---
name: new-feature
description: 기존 도메인(member/auth)에 새 기능(UseCase → Service → Api → Controller)을 추가한다
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

기존 도메인에 새 기능을 추가한다.

기능 설명: $ARGUMENTS

> 계층별 타입 어휘와 UseCase 분리 판단 기준은 `architecture` 스킬을 그대로 따른다.

## Work Order

1. **UseCase 정의** (application): `architecture` 스킬의 "판단 기준"(단순 조회/CRUD면 기존 Query/Command Service 유지, 트리거·부수효과가 다른 흐름이면 별도 `*UseCase`/`*Service` 분리)으로 신규 UseCase 필요 여부를 먼저 판단한다.
2. **Service 구현**: `@CommandService`(쓰기)/`@QueryService`(조회) 부착, 메서드 단위 `@Transactional` 금지.
3. **Api 인터페이스 + Controller** (presentation): `*Request.toCommand()`/`toQuery()`로 변환, `*Response.from(Result)`로 응답 생성.
4. **DTO 작성** (presentation): 필요한 `*Request`/`*Response` 추가.
5. **테스트 작성**: `test-writer` 에이전트를 호출해 `testing` 스킬 규칙대로 테스트를 작성한다.
6. **검증**: `./gradlew test`.

## Principles

- 도메인 로직은 도메인 객체 메서드에 위치시킨다.
- Service는 오케스트레이션만 담당한다.
- 크로스 도메인 조율이 필요하면 대상 도메인의 `*UseCase` 인터페이스를 직접 의존한다 (별도 Facade 없음).
