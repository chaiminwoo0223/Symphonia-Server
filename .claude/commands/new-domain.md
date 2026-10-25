---
name: new-domain
description: 새 도메인의 4계층(domain/application/presentation/infrastructure) 패키지와 기본 골격을 스캐폴딩한다
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

새 도메인 '$ARGUMENTS'를 `architecture` 스킬 규칙에 따라 스캐폴딩한다.

## 절차

1. `architecture` 스킬을 로드하고, `src/main/java/com/symphonia` 아래 기존 도메인 패키지 구조를 확인한다.
2. 새 도메인이 `architecture` 스킬 프로젝트 구조 절의 `shared` 패키지 재검토 조건에 해당하는지 먼저 판단한다. 해당하면 진행 전에 사용자에게 확인한다.
3. `domain-scaffolder` 에이전트를 호출해 4계층 패키지와 골격을 생성한다.
4. `./gradlew compileJava`로 컴파일을 확인한다.
5. 생성 결과를 한국어로 요약한다.

도메인 이름: $ARGUMENTS
