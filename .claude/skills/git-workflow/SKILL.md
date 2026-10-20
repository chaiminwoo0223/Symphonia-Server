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
| `develop` | 통합 (PR 대상). 직접 커밋은 긴급 `[HotFix]` 대응에만 허용 (`commit-push` 스킬 참고) |
| `<type>/이슈번호` | 이슈 작업 (`develop`에서 분기). 접두어는 그 작업의 주된 커밋 type을 그대로 쓴다 (예: `feat/9`, `refactor/22`, `perf/79`, `docs/101`, `chore/29`). 이슈 번호 앞에 `#`을 붙이지 않는다 — `check-commit-issue-number.sh`의 브랜치 매칭 정규식(`^[a-z]+/([0-9]+)$`)도 `#` 없는 형식만 인식한다 |

## Branch 보호 (Ruleset)

`develop`, `main`에는 GitHub Ruleset이 걸려 있다(2026-10-20, 이슈 #85).

| 규칙 | `develop` | `main` |
|---|---|---|
| PR 필수, 승인 1명 | ✅ | ✅ |
| 승인 뒤 새 커밋이 push되면 기존 승인 무효화 | ✅ | ✅ |
| 모든 리뷰 대화(conversation) Resolve 필수 | ✅ | ✅ |
| force push 차단, 브랜치 삭제 차단 | ✅ | ✅ |
| Bypass | Repository admin (Always) | 없음 |

- 저장소가 개인 계정 소유라 Maintain 역할이 없다. 그래서 bypass 대상은 Repository admin 역할 하나뿐이다.
- admin bypass는 `develop`에 `[HotFix]`를 직접 push할 때만 쓴다. PR 머지 화면의 우회 체크박스로 승인이나 코멘트 Resolve를 건너뛰지 않는다.
- admin이 아닌 협업자는 bypass할 수 없으므로 `[HotFix]`도 PR로 보낸다.
- `develop`의 bypass는 위 규칙 전부에 적용된다. 그래서 admin에게는 force push 차단과 삭제 차단도 GitHub가 강제하지 않는다. 이를 막는 것은 위의 사용 범위 규칙과 `commit-push` 스킬의 `--force` 금지다.
- `main`은 bypass가 없어 누구도 직접 push하거나 승인 없이 머지할 수 없다.

## Commit & Push

커밋 전 검증, 커밋 단위(관심사 단위 분리/묶음), 메시지 포맷·예시, push 규칙, push 게이트 동작은 모두 `commit-push` 스킬 참고.

## PR 생성

PR 대상, 제목·본문 구성, 생성 절차는 `create-pr` 스킬 참고. PR 제목은 `[Type] 설명` 형식(이슈 번호 없음)이며 커밋 메시지 포맷(`[#이슈번호] type: 설명`)과는 다르다.

## GitHub 이슈

GitHub 이슈 생성 규칙(템플릿 매핑, 제목·라벨, 표현 규칙, 생성 절차)은 `create-issue` 스킬 참고. 브랜치명(`<type>/이슈번호`)과 커밋 메시지(`[#이슈번호] type: 설명`)는 그렇게 생성된 이슈 번호를 그대로 쓴다.
