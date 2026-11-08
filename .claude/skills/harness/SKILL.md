---
name: harness
description: Load when editing harness artifacts. CLAUDE.md, settings.json, .claude/hooks, .claude/scripts, .claude/agents, .claude/skills를 추가하거나 고칠 때, 하네스 계층 매핑, 결정론적 검사와 LLM 판단의 경계, 분량 예산, 평가 시나리오.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Harness Rules

`.claude/` 설정과 `CLAUDE.md`는 에이전트 행동을 프로그래밍하는 실행 가능한 단일 진실 소스다. 소스 코드처럼 계층화하고, 단일 책임으로 나누고, 리뷰한다. 근거와 이력은 `docs/decisions/harness.md`에 있다.

## 아티팩트와 책임

| 아티팩트 | 대응 | 책임 |
|---|---|---|
| `CLAUDE.md` | 매니페스트 | 무엇이 있고 언제 로드하는지만 담는 라우터. 판단 규칙의 본문은 스킬에 둔다 |
| `settings.json` | 거버넌스 게이트 | 위험 명령을 `permissions.deny`로 강제한다. 차단 대상은 `commit-push` 스킬 참고 |
| `.claude/hooks/*` | 인터셉터 | 도구 호출 전후를 가로채 컨벤션대로 교정한다. 훅별 역할은 각 스크립트의 머리 주석, 커밋과 push 게이트는 `commit-push`, PR 생성 게이트는 `create-pr` 스킬이 단일 소스다 |
| 사용자 호출 전용 스킬(`/new-domain`, `/new-feature`, `/run-checks`) | Controller | 사용자가 트리거하는 절차의 진입점. `disable-model-invocation: true`로 Claude가 스스로 로드하지 않는다 |
| 서브에이전트(`domain-scaffolder`, `code-reviewer`, `test-*`) | Service | 격리된 컨텍스트에서 여러 단계를 조율한다 |
| `.claude/skills/<name>/SKILL.md` | SRP 컴포넌트 | 하나의 지식 영역 = 그 규칙의 단일 소스 |
| `.claude/scripts/*` | 결정론적 코어 | 흔들리면 안 되는 검증을 코드로 실행한다 |
| `docs/decisions/*.md` | 설계 기록 | 규칙의 근거, 택하지 않은 안, 이력. 기본으로 로드하지 않는다 |

## 운영 원칙

- 현재 작업에 필요한 컨텍스트만 로드한다(progressive disclosure). `CLAUDE.md`의 작업별 로딩 표가 그 장치다.
- 결정론적으로 판정할 수 있는 규칙은 스크립트로 강제하고, 판단이 섞이는 규칙은 `code-reviewer`가 체크리스트로 검토한다.
  - 포맷: Spotless + `post-edit-format.sh`
  - 테스트 블록 주석과 `should` 접두어: `check-test-conventions.sh`
  - import와 선언으로 판정하는 계층 규칙: `check-architecture.sh`
  - 스킬과 `CLAUDE.md` 분량: `check-harness-budget.sh`
- 스크립트의 에러 메시지에는 위치와 고치는 방법을 함께 적는다. 규칙은 위반했을 때만 컨텍스트에 들어오게 한다.
- 검증 스크립트는 성공하면 한 줄만 출력한다. 실패하면 실패한 단계와 필요한 로그만 출력한다.
- 되돌리기 어렵거나 대외적인 행동(삭제, 배포, force-push, 게시)은 확인 없이 진행하지 않는다.
- 하네스 아티팩트도 리뷰 대상이다. 스킬이 토큰을 과도하게 쓰거나 체크리스트를 잘못 적용하면 그 스킬 문서를 고친다.

## 분량 예산

- 스킬에는 현재 규칙만 명령형으로 둔다. 근거, 택하지 않은 안, 측정 결과, 이력은 `docs/decisions/`에 둔다(`design-decision` 스킬).
- 예산은 `.claude/scripts/check-harness-budget.sh`가 검사하고 `check-all.sh`가 실행한다. 한도 값은 스크립트가 단일 소스다.
- 예산을 넘으면 한도를 올리기 전에 근거와 이력이 섞였는지, 하위 파일로 나눌 주제가 있는지부터 본다.

## 서브에이전트

- 판단이 깊지 않은 절차형 에이전트(`test-validator`, `domain-scaffolder`)는 `model: sonnet`을 지정한다.
- `code-reviewer`에는 호출자가 `git diff`의 원문을 그대로 넘긴다. 요약해서 넘기지 않는다.

## 평가

하네스를 바꾸면 `docs/decisions/harness.md`의 평가 시나리오 3개로 변경 전후의 토큰 수, 소요 시간, 검증 결과를 비교하고 PR 본문에 남긴다.
