---
name: design-decision
description: Load when a design discussion among multiple viable alternatives converges on a decision. 설계 결정을 어디에, 어떤 형식으로 기록할지.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# 설계 결정 기록

결정은 두 곳에 나눠 기록한다. 스킬에는 지금 지켜야 할 규칙만 명령형으로 둔다. `docs/decisions/<주제>.md`에는 그 규칙의 근거, 택하지 않은 안, 재검토 조건, 측정 결과를 둔다. 스킬은 작업마다 로드되므로 짧아야 하고, 근거는 같은 논의가 다시 나올 때만 읽으면 된다.

## 언제 기록하는가

여러 설계 대안을 놓고 논의하다가 하나로 결론이 나는 순간에 기록한다. 결정만 남기고 근거를 생략하지 않는다. 왜 다른 대안을 택하지 않았는지까지 남겨야 같은 논의가 반복되지 않는다.

## 어디에 기록하는가

| 결정 주제 | 규칙 (스킬) | 근거 (`docs/decisions/`) |
|---|---|---|
| 계층 구조, 크로스 도메인 규칙, 검증 범위 | `architecture/SKILL.md` | `architecture.md` |
| 인증, 토큰, 보안 wiring | `architecture/SKILL.md`의 인증 절 | `auth.md` |
| catalog 분리와 다른 도메인에 여는 UseCase | `architecture/SKILL.md` | `catalog.md` |
| DB 스키마, 마이그레이션, 프로파일 설정, 카탈로그 적재 | `architecture/persistence.md` | `persistence.md` |
| 추천 점수, 후보 선정, Harmonize 규칙표, 추천 성능 | `architecture/pairing.md` | `pairing.md` |
| 정산 모델, 개인정보 처리, 정산 API | `architecture/gathering.md` | `gathering.md` |
| 발송 예약, 재시도, 큐, 리마인드 | `architecture/notification.md` | `notification.md` |
| 예외, 에러 코드 | `error-handling` | `error-handling.md` |
| 테스트 전략 | `testing` | `testing.md` |
| 하네스(스킬, 훅, 스크립트, 에이전트) | `harness` | `harness.md` |

- 커밋/브랜치(`commit-push`), Ruleset, CI, 리뷰 도구(`git-workflow`), 이슈와 PR 절차(`create-issue`, `create-pr`), 포맷터와 네이밍(`code-style`) 결정은 아직 그 스킬에 날짜와 이슈 번호를 붙여 인라인으로 남긴다.
- 한 결정이 도메인 규칙과 영속성 기술을 함께 다루면 결정의 이유가 속한 쪽에 적는다. 예를 들어 `drink.harmonizes` 배열 컬럼은 추천 성능 때문에 정했으므로 `pairing.md`에 있다.
- 새 도메인의 결정은 처음부터 `architecture/<도메인>.md`와 `docs/decisions/<도메인>.md`에 적는다. 하위 파일을 만들면 `architecture/SKILL.md`의 하위 문서 표에 언제 읽는지 한 줄로 적고 `description`에도 주제를 더한다.

## 어떻게 기록하는가

스킬에는 규칙을 명령형 문장으로 적는다. 날짜, 이슈 번호, 근거, 택하지 않은 안은 넣지 않는다.

```
- 정산 건은 모임 생성 후 48시간 안에만 기록할 수 있다.
```

`docs/decisions/<주제>.md`에는 주제별 절 아래에 기존 형식으로 적는다.

```
**(YYYY-MM-DD 결정, 이슈 #번호)**: <결정 내용>. <근거>.
```

- 대안이 여럿이었다면 왜 다른 대안을 택하지 않았는지 한 문장으로 남긴다.
- 재검토 조건이 있으면 명시한다.
- 후속 결정이 앞 결정을 바꾸면 스킬의 규칙을 지금 맞는 내용으로 고친다. 결정 기록에는 새 문단을 더하고, 앞 문단의 바뀐 부분 바로 뒤에 `(#번호에서 변경)`을 붙인다. 앞에서 기각한 대안을 채택했다면 그 문장 뒤에 `(#번호에서 채택)`을 붙인다.
- 규칙과 근거는 같은 커밋에서 함께 고친다. 한쪽만 고치면 두 문서가 어긋난다.

## 절차

1. 논의 중 결정이 나오면 그 자리에서 위 표로 스킬과 결정 기록 파일을 정한다. 인라인으로 남기는 주제는 그 스킬만 정한다. 이 결정이 바꾸는 앞 규칙과 앞 결정 문단도 함께 찾는다.
2. 사용자에게 스킬에 넣을 규칙 문장과 결정 기록 문단을 보여 주고 승인받는다. 인라인으로 남기는 주제는 날짜와 이슈 번호를 붙인 규칙 문장만 보여 준다.
3. 승인된 문구를 두 파일에 반영한다. 인라인으로 남기는 주제는 그 스킬에만 반영한다. 진행 중인 작업 커밋에 얹거나, 결정 반영만 단독으로 할 때는 `[#이슈번호] docs: <스킬명> 스킬에 <결정 요지> 반영` 형태로 커밋한다(`commit-push` 스킬 참고).
