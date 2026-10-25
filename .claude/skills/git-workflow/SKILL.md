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
| `<type>/이슈번호` | 이슈 작업 (`develop`에서 분기). 접두어는 그 작업의 주된 커밋 type을 그대로 쓴다 (예: `feat/9`, `refactor/22`, `perf/79`, `docs/101`, `chore/29`). 이슈 번호 앞에 `#`을 붙이지 않는다 — 이슈번호 검사 훅과 TODO 검사 훅이 함께 쓰는 `.claude/hooks/lib.sh`의 `branch_issue_number` 정규식(`^[a-z]+/([0-9]+)$`)도 `#` 없는 형식만 인식한다 |

## Branch 보호 (Ruleset)

`develop`, `main`에는 GitHub Ruleset이 걸려 있다(2026-10-20, 이슈 #85).

| 규칙 | `develop` | `main` |
|---|---|---|
| PR 필수, 승인 1명 | ✅ | ✅ |
| 승인 뒤 새 커밋이 push되면 기존 승인 무효화 | ✅ | ✅ |
| 모든 리뷰 대화(conversation) Resolve 필수 | ✅ | ✅ |
| force push 차단, 브랜치 삭제 차단 | ✅ | ✅ |
| CI `check` job 통과 필수 (브랜치 최신화는 요구하지 않음, 이슈 #112) | ✅ | ✅ |
| Bypass | Repository admin (Always) | 없음 |

- 저장소가 개인 계정 소유라 Maintain 역할이 없다. 그래서 bypass 대상은 Repository admin 역할 하나뿐이다.
- admin bypass는 `develop`에 `[HotFix]`를 직접 push할 때만 쓴다. PR 머지 화면의 우회 체크박스로 승인, 코멘트 Resolve, CI `check` 실패를 건너뛰지 않는다.
- admin이 아닌 협업자는 bypass할 수 없으므로 `[HotFix]`도 PR로 보낸다.
- `develop`의 bypass는 위 규칙 전부에 적용된다. 그래서 admin에게는 force push 차단과 삭제 차단도 GitHub가 강제하지 않는다. 이를 막는 것은 위의 사용 범위 규칙과 `commit-push` 스킬의 `--force` 금지, 그리고 그 금지를 강제하는 `settings.json`의 `permissions.deny`다. `permissions.deny`는 force push만 막고 브랜치 삭제는 막지 않는다.
- admin이 `develop`에 직접 push한 `[HotFix]`는 CI를 거치지 않는다. Claude Code 세션에서 push하면 `pre-push-check.sh`가 검증하고, 세션 밖에서 push한다면 push 전에 `check-all.sh`를 직접 실행한다. 이 커밋은 이후 `develop` → `main` PR의 CI에서 다시 검증된다.
- `main`은 bypass가 없어 누구도 직접 push하거나 승인 없이 머지할 수 없다.

## Commit & Push

커밋 전 검증, 커밋 단위(관심사 단위 분리/묶음), 메시지 포맷·예시, push 규칙, push 게이트 동작은 모두 `commit-push` 스킬 참고.

## PR 생성

PR 대상, 제목·본문 구성, 생성 절차는 `create-pr` 스킬 참고. PR 제목은 `[Type] 설명` 형식(이슈 번호 없음)이며 커밋 메시지 포맷(`[#이슈번호] type: 설명`)과는 다르다.

## CodeRabbit 리뷰

`develop` 대상 PR에는 CodeRabbit이 자동으로 리뷰를 남긴다(2026-10-25, 이슈 #86).

| 리뷰어 | 시점 | 범위 |
|---|---|---|
| `code-reviewer` 에이전트 | `gh pr create` 전 (`create-pr` 스킬) | 계층 의존 방향, 계층별 타입 어휘, 네이밍, 멤버 선언 순서, 테스트 컨벤션 |
| CodeRabbit | PR 생성 후와 새 커밋 push마다 | 일반 버그, 보안, 동시성, null 처리, 자원 누수, 성능, 마이그레이션 운영 위험, 셸과 워크플로 오류 |

- 설정은 저장소 루트의 `.coderabbit.yaml`에서만 바꾼다. 대시보드 설정은 쓰지 않는다. 그래서 저장소를 Organization으로 옮겨도 설정이 그대로 따라간다.
- CodeRabbit은 리뷰 대상 PR의 head 브랜치에 있는 `.coderabbit.yaml`을 읽는다. 설정 변경은 그 설정을 바꾸는 PR 자체에서 바로 확인할 수 있다.
- 핵심 제약은 CodeRabbit의 검토 대상이 아니라 가드레일이다. `path_instructions`에는 제약과 반대되는 수정을 제안하지 말라는 짧은 목록만 두고, 근거는 `code_guidelines`로 `architecture`, `error-handling` 스킬을 직접 읽게 한다. 규칙을 YAML에 복사하면 스킬과 두 벌로 갈라지기 때문이다. 스킬 문서는 제안을 거르는 근거로만 쓰고 위반 지적에는 쓰지 않는다고 지시문에 적어 둔다. `code-style`, `testing`은 `code-reviewer`만 보는 컨벤션이라 읽히지 않는다.
- 위 표의 CodeRabbit 범위는 `.coderabbit.yaml`의 `**/*` 지시문에도 적혀 있다. 범위를 바꿀 때는 두 곳을 함께 고친다.
- `develop` → `main` 릴리스 PR은 자동 리뷰하지 않는다. 이미 `develop` 대상 PR에서 리뷰한 커밋을 다시 보는 셈이라서다. CodeRabbit은 기본 브랜치와 `auto_review.base_branches`에 적은 브랜치만 자동 리뷰하는데, 저장소의 기본 브랜치가 `develop`이고 `main`은 목록에 없다. 기본 브랜치를 `main`으로 바꾸면 릴리스 PR도 리뷰 대상이 되므로 바꾸지 않는다.
- `main` Ruleset(PR 필수, 승인 1명, CI `check` 통과)은 코드 리뷰가 아니라 릴리스 승인과 CI 게이트 역할이다. 각각 통과한 `develop` PR들을 합친 결과를 CI가 한 번 더 검증하고, 승인자는 배포 시점을 확인한다.
- CodeRabbit은 승인하거나 변경을 요청하지 않고 코멘트만 남긴다. 머지에 필요한 승인 1명은 여전히 사람이 한다.
- Ruleset이 모든 리뷰 대화의 Resolve를 요구하므로 CodeRabbit 코멘트도 Resolve해야 머지할 수 있다. 반영하지 않는 코멘트는 이유를 답글로 남기고 Resolve한다.

## GitHub 이슈

GitHub 이슈 생성 규칙(템플릿 매핑, 제목·라벨, 표현 규칙, 생성 절차)은 `create-issue` 스킬 참고. 브랜치명(`<type>/이슈번호`)과 커밋 메시지(`[#이슈번호] type: 설명`)는 그렇게 생성된 이슈 번호를 그대로 쓴다.
