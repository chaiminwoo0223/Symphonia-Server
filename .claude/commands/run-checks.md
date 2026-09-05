---
name: run-checks
description: PR 전 전체 검증을 실행한다 (./gradlew clean build)
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

PR 전 전체 검증을 실행한다. (`./.claude/scripts/check-all.sh`와 동일한 절차)

1. `./gradlew clean build`: 빌드 + 전체 테스트

실패하면 원인을 분석해 수정하고 재실행한다. 전체 통과하면 결과를 한국어로 요약한다.
