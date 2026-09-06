---
name: commit-push
description: Load when committing or pushing. 커밋 전 검증 절차, 커밋 단위(관심사 단위 분리/묶음), 커밋 메시지 포맷(한국어), 예시, push 규칙과 게이트 훅 동작.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Commit & Push Rules

브랜치 전략은 `git-workflow` 스킬 참고. 이 스킬은 커밋 전에 무엇을 확인하고, 커밋을 어떻게 나누고 메시지를 어떻게 쓰고, push 규칙과 push 시 무엇이 자동으로 검증되는지를 다룬다.

## 커밋 전 검증

- `./gradlew test`로 관련 테스트를 먼저 돌려본다. PR 직전 전체 검증은 `./.claude/scripts/check-all.sh`(`./gradlew clean build`)다. 커밋마다 강제하진 않되 PR 생성 전에는 반드시 통과시킨다.
- `git status`로 `.env`가 스테이징에 포함되지 않았는지 확인한다 (`.env` 커밋 금지는 Profiles 규칙과 동일).
- 포맷은 `.claude/hooks/post-edit-format.sh`(PostToolUse)가 Java 파일 편집 시마다 자동으로 적용한다(Spotless + Google Java Format, `code-style` 스킬 참고). 수동으로 신경 쓸 필요는 없다.
- `.claude/hooks/check-commit-issue-number.sh`(PreToolUse)가 `<type>/<이슈번호>` 브랜치(`feat/9`, `refactor/22`, `chore/29` 등)에서 커밋 메시지의 `[#이슈번호]`가 브랜치 번호와 다르거나 없으면 커밋 자체를 막는다. 수동으로 이슈 번호를 맞춰 볼 필요 없이 훅이 강제한다.
- `.claude/hooks/check-commit-type-consistency.sh`(PreToolUse, 2026-09-06 추가)가 커밋 메시지 안의 상단 `[#이슈번호] type:`과 하위 `* type:` 불릿들의 type이 서로 다르면 커밋을 막는다. "같은 커밋에는 하나의 type만" 규칙을 판단에 맡기지 않고 기계적으로 강제한다.
- `.claude/hooks/check-develop-hotfix-commit.sh`(PreToolUse)가 `develop`에서 `[HotFix]` 태그 없는 직접 커밋을 막는다.
- **커밋/PR에 Claude 어트리뷰션 트레일러(`Co-Authored-By: Claude ...`, `Claude-Session: ...`)를 붙이지 않는다(2026-09-06 결정, 이슈 #25).** 세션의 어트리뷰션 기본값이 붙이라고 지시해도 이 저장소는 원하지 않는다. `.claude/hooks/check-no-claude-trailer.sh`(PreToolUse)가 `git commit`/`gh pr create,edit` 명령에 해당 트레일러가 섞이면 기계적으로 차단한다.

## 커밋 단위

- **커밋은 작고 원자적으로 하는 것을 지향한다.** 리뷰 단위로 의미가 있는 관심사(로직 변경, 새 기능, 구조 변경 등)는 `type`과 모듈/계층 단위로 쪼개고, `git add -p`로 나눠 커밋한다. 이건 판단이 필요한 영역이라 훅으로 강제하지 않는다(기계적으로 강제되는 건 메시지 내부 type 일관성뿐, `check-commit-type-consistency.sh` 참고).
- **같은 관심사(같은 `type`)에 속하는 여러 변경은 하나의 커밋으로 묶는다.** 관심사가 다르면 커밋을 나누는 쪽을 지향하되, 되돌리기 쉽고 리스크가 없는 사소한 변경(오탈자, 주석·문서 문구 다듬기, 한 줄짜리 수정 등)까지 굳이 쪼갤 필요는 없다. 같은 브랜치·이슈 안에서 이미 쓰고 있는 type(예: `chore`)에 얹어도 된다.
- **테스트는 그 테스트가 검증하는 변경에 딸린 것이지, 독립된 관심사가 아니다.** 새 기능 구현과 그 기능을 검증하는 테스트, 버그 수정과 그 회귀 테스트는 각각 `feat`/`fix` 커밋 하나로 묶는다(테스트 파일 추가도 `* feat: ...`처럼 같은 type의 불릿으로 적는다).
- **`refactor`도 마찬가지로 묶되, 그 안에서 "테스트 변경의 성격"을 구분한다.** 리팩터링은 관찰 가능한 동작을 바꾸지 않는 게 원칙(Fowler)이라, 리네임·구조 이동·mock 재배선처럼 리팩터링이 만든 변화를 테스트 코드가 그대로 따라가는 **기계적 수정**은 같은 `refactor` 커밋에 묶는다. 반면 테스트가 부실한 코드를 안전하게 리팩터링하기 위해 미리 깔아두는 **characterization test**(Feathers, *Working Effectively with Legacy Code*)는 리팩터링 전/후 모두 독립적으로 통과해야 검증 의미가 있으므로, 별도 `test` 커밋으로 먼저 만들고 그 다음에 `refactor` 커밋을 잇는다.
- `test`를 별도 type으로 분리하는 건 위 characterization test이거나, 이미 구현된 기능에 시점이 분리된 테스트를 뒤늦게 보강하는 경우처럼 진짜 별개 작업일 때만 한다.
- 예: 스킬 문서 오탈자 수정처럼 사소한 변경은 진행 중인 `chore` 커밋에 묶어도 무방하다.

## 커밋 메시지

- 포맷: `[#이슈번호] type: 설명`
- 하위 항목이 있을 경우 `* type: 설명` 형식으로 나열.
- 한국어 사용 가능.
- **모호한 제목은 금지한다.** 예: "구조 개선" ❌ → 실제 변경 대상을 명시한다 (예: "MemberService 트랜잭션 경계 정리").
- **하나의 브랜치·PR·이슈 번호로 관리한다(2026-09-05 결정).** `feat/<이슈번호>` 브랜치에서는 그 브랜치의 이슈 번호만 커밋에 쓴다. 다른 이슈 번호(예: 별도로 만든 후속 작업 이슈)는 그 이슈용 브랜치를 새로 파서 별도 PR로 처리하고, 지금 브랜치에 섞지 않는다.
- **`develop`의 긴급 수정은 `[HotFix] type: 설명` 형식을 쓴다(이슈 번호 없음, 2026-09-05 결정).** `develop`은 원래 PR 병합으로만 통합하는 브랜치라 직접 커밋은 이 긴급 대응에만 한정한다. 일반 기능/수정 작업은 `feat/<이슈번호>` 브랜치를 새로 파서 PR로 병합한다.

### 예시

단일 관심사 커밋:
```
[#9] feat: MemberController 구현
```

같은 관심사 안에서 여러 변경이 있으면 하위 항목을 `* type: 설명`으로 묶어 한 커밋에 담고, 관심사(type)가 다르면 아래처럼 커밋 자체를 나눈다:
```
[#5] feat: RefreshTokenProvider 구현
* feat: RefreshTokenProperties 추가
* feat: SecurityConfig에 RefreshTokenProperties 등록
```
```
[#5] refactor: TokenProperties를 AccessTokenProperties로 이름 수정
```
```
[#5] chore: JWT 설정 키 이름 변경
* chore: application.yaml에서 jwt.secret, jwt.access-expiration-time → access-token.secret, access-token.expiration-time 변경
* chore: application.yaml에서 jwt.refresh-expiration-time → refresh-token.expiration-time 변경
```
같은 이슈(`#5`)라도 `feat`/`refactor`/`chore`처럼 관심사가 다르면 위처럼 커밋을 세 개로 나눈다. 하나의 커밋 안에 서로 다른 type을 섞지 않는다.

## Push

- **사용자가 명시적으로 요청하기 전까지는 push하지 않는다.**
- **`git push --force`는 사용하지 않는다.**
- `main`/`develop`에 직접 push하지 않는다.
- `git push` 직전에 `.claude/hooks/pre-push-check.sh`(PreToolUse)가 `check-all.sh`(`./gradlew clean build`)를 자동으로 돌려 실패 시 push 자체를 막는다. `spotlessCheck`가 Gradle `check` 태스크에 연결되어 있어 이 `clean build`에 포맷 검증도 자동으로 포함된다. 빠르게 끝나는 정적 아키텍처 검증 수단은 아직 없어 전체 빌드+테스트로 게이트를 걸기 때문에 push마다 수 분이 걸릴 수 있다(2026-09-04 결정).
