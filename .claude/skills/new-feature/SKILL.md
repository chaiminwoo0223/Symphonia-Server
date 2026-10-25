---
name: new-feature
description: 기존 도메인에 새 기능(UseCase → Service → Api → Controller)을 추가한다
disable-model-invocation: true
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

기존 도메인에 새 기능을 추가한다.

기능 설명: $ARGUMENTS

> 계층 규칙, UseCase 분리 판단 기준, DDD 원칙, 크로스 도메인 참조는 `architecture` 스킬을 그대로 따른다.

## Work Order

1. **UseCase 정의** (application): `architecture` 스킬의 "판단 기준"(단순 조회/CRUD면 기존 Query/Command Service 유지, 트리거·부수효과가 다른 흐름이면 별도 `*UseCase`/`*Service` 분리)으로 신규 UseCase 필요 여부를 먼저 판단한다.
2. **Service 구현**: `architecture` 스킬의 트랜잭션 / CQRS 절에 따라 `@CommandService`(쓰기)/`@QueryService`(조회)를 부착한다.
3. **Api 인터페이스 + Controller** (presentation): `*Request.toCommand()`/`toQuery()`로 변환, `*Response.from(Result)`로 응답 생성.
4. **DTO 작성** (presentation): 필요한 `*Request`/`*Response` 추가.
5. **테스트 작성**: `test-writer` 에이전트를 호출해 `testing` 스킬 규칙대로 테스트를 작성한다.
6. **검증**: `./gradlew test`.
