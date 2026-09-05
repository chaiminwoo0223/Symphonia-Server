---
name: git-workflow
description: Load when branching/committing/pushing/creating PRs or issues. Branch부터 PR 생성까지 전체 흐름과 각 단계 규칙의 위치.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Git Workflow

이슈 생성부터 PR 생성까지, 변경 사항을 진행하는 전체 흐름과 각 단계 규칙이 어디에 있는지를 정리한다.

## Branch 전략

| 브랜치 | 용도 |
|---|---|
| `main` | 운영 배포 |
| `develop` | 통합 (PR 대상) |
| `feat/#이슈번호` | 기능 개발 (`develop`에서 분기) |

## Commit & Push

커밋 전 검증, 커밋 단위(관심사 단위 분리/묶음), 메시지 포맷·예시, push 규칙, push 게이트 동작은 모두 `commit-push` 스킬 참고.

## PR 생성

PR 대상, 제목·본문 구성, 생성 절차는 `create-pr` 스킬 참고. PR 제목은 `[Type] 설명` 형식(이슈 번호 없음)이며 커밋 메시지 포맷(`[#이슈번호] type: 설명`)과는 다르다.

## GitHub 이슈

GitHub 이슈 생성 규칙(템플릿 매핑, 제목·라벨, 표현 규칙, 생성 절차)은 `create-issue` 스킬 참고. 브랜치명(`feat/#이슈번호`)과 커밋 메시지(`[#이슈번호] type: 설명`)는 그렇게 생성된 이슈 번호를 그대로 쓴다.
