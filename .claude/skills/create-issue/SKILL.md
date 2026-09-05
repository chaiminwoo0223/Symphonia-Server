---
name: create-issue
description: Load when creating a GitHub issue. 이슈 템플릿 타입 매핑, 제목/라벨 규칙, gh issue create 절차.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# GitHub 이슈 생성

`.github/ISSUE_TEMPLATE/`의 폼 템플릿을 그대로 따라 이슈를 생성한다. 생성된 이슈 번호가 브랜치명에 쓰이는 규칙은 `git-workflow` 스킬에, 커밋 메시지에 쓰이는 규칙은 `commit-push` 스킬에 있다. 이 스킬은 타입별 템플릿 매핑, 제목·라벨·표현 규칙, 실제 생성 절차를 다룬다.

## 타입 → 템플릿 매핑

| 타입 | 템플릿 파일 | 제목 접두사 | 라벨 |
|---|---|---|---|
| 기능 추가 | `feature.yml` | `[Feature] ` | `✨ feature` |
| 버그 | `bug.yml` | `[Bug] ` | `🐛 bug` |
| 리팩토링 | `refactor.yml` | `[Refactor] ` | `♻️ refactor` |
| 설정 | `chore.yml` | `[Chore] ` | `⚙️ chore` |
| 성능 개선 | `performance.yml` | `[Performance] ` | `⚡️ performance` |
| 문서화 | `documentation.yml` | `[Documentation] ` | `📄 documentation` |

- 6개 템플릿 모두 필드 구성이 동일하다. `설명`(무엇을 왜 하는지)과 `할 일 목록`(체크박스 TODO) 두 필드를 요구하며, 둘 다 필수 항목이다. 
- 정확한 제목 접두사와 라벨 문자열은 위 표를 신뢰하되, 템플릿 파일이 바뀌었을 가능성에 대비해 실제 생성 전에 해당 `.yml` 파일의 `title`/`labels` 값을 다시 확인한다.

## 절차

1. 사용자 설명에서 타입을 판단한다. 애매하면 어떤 타입인지 먼저 확인한다.
2. 사용자와 함께 `설명`과 `할 일 목록` 내용을 채운다. 설명에는 무엇을 왜 하는지 쓰고, 할 일 목록에는 체크박스로 나눌 수 있는 작업 단위를 쓴다.
3. 이슈를 생성한다.
   ```bash
   gh issue create \
     --title "[Feature] <제목>" \
     --label "✨ feature" \
     --body "$(cat <<'EOF'
   ## 📌 이슈 내용 설명
   <설명>

   ## ✅ TODO
   - [ ] 작업 1
   - [ ] 작업 2
   EOF
   )"
   ```
   타입에 맞게 `--title` 접두사, `--label`, 헤더 아래 내용을 위 매핑표대로 바꾼다.
4. 생성된 이슈 번호를 사용자에게 알린다. 이 번호는 이후 브랜치명(`feat/#이슈번호`, `git-workflow` 참고)과 커밋 메시지(`[#이슈번호] type: 설명`, `commit-push` 참고)에 쓰인다.

## 예시

실제 발급된 Chore 타입 이슈다. 제목, `설명`, `할 일 목록`을 채울 때 이 형태를 그대로 따른다.

제목: `[Chore] SecurityFilterChain 및 CORS 설정 구성`

```markdown
## 📌 이슈 내용 설명

### 설명

- Spring Security의 `SecurityFilterChain`을 구성하고, 어드민 웹 클라이언트를 위한 CORS 정책을 설정합니다.
- `JwtAuthenticationFilter`를 `SecurityFilterChain`에 등록해 인증/인가 흐름을 완성합니다.

## ✅ TODO

### 할 일 목록

- [ ] SecurityFilterChain에 Stateless 세션 정책 적용 및 CSRF 비활성화
- [ ] JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 앞에 등록
- [ ] CustomAuthenticationEntryPoint, CustomAccessDeniedHandler를 예외 핸들러로 등록
- [ ] 엔드포인트별 인가 규칙 적용 (permitAll / authenticated)
```

**스타일 규칙**

- 제목은 접두사 뒤에 완결된 문장이 아니라 명사형으로 끝나는 구체적인 대상을 쓴다. 예: "SecurityFilterChain 및 CORS 설정 구성". "~개선", "~작업" 같은 막연한 제목은 쓰지 않는다.
- `설명`은 불릿 리스트로 쓴다. 불릿 하나는 하나의 하위 목표만 가리키고, 관련 클래스·컴포넌트 이름을 구체적으로 언급하며, "-합니다"체 완결문으로 끝낸다.
- `할 일 목록`은 체크박스 리스트로 쓴다. 각 항목은 완결된 문장이 아니라 "무엇을 어떻게 적용/등록/구성한다"는 짧은 동작 구문으로 쓰고, 체크박스 하나는 그 자체로 검증 가능한 작업 단위여야 한다.
- 두 필드 모두 "~개선", "~정리" 같은 추상적 표현 대신 실제 클래스명·설정값·엔드포인트를 명시한다.

## 원칙

- 템플릿 구조(섹션 제목, 필수 필드)를 임의로 바꾸지 않는다.
- 제목과 본문 모두 한국어로 작성한다.
- `설정` 타입 이슈는 `chore` 라벨을 쓴다 (`feat`가 아니다). 제목·설명에서는 "구현"보다 "구성"이라는 표현을 우선한다.
