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

## 커밋 단위

- **커밋은 작고 원자적으로 한다.** 하나의 커밋에 하나의 관심사만 담는다. `type`과 모듈/계층 단위로 쪼개고, 여러 관심사가 섞인 변경은 `git add -p`로 나눠 커밋한다.
- **같은 관심사(같은 `type`)에 속하는 여러 변경은 하나의 커밋으로 묶는다.** 관심사가 다르면 커밋 자체를 나눈다. 즉 같은 `type` 안에서는 묶고, `type`이 갈리면 쪼갠다.
- 예: 도메인 로직 변경과 테스트 추가가 같이 있으면 `feat`/`test`로 분리 커밋.

## 커밋 메시지

- 포맷: `[#이슈번호] type: 설명`
- 하위 항목이 있을 경우 `* type: 설명` 형식으로 나열.
- 한국어 사용 가능.
- **모호한 제목은 금지한다.** 예: "구조 개선" ❌ → 실제 변경 대상을 명시한다 (예: "MemberService 트랜잭션 경계 정리").

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
