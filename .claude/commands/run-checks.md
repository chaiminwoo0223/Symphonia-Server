---
name: run-checks
description: PR 전 전체 검증(check-all.sh)을 실행한다
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

PR 전 전체 검증으로 `./.claude/scripts/check-all.sh`를 실행한다. 검증 단계의 구성은 이 스크립트가 단일 소스다.

실패하면 원인을 분석해 수정하고 재실행한다. 전체 통과하면 결과를 한국어로 요약한다.
