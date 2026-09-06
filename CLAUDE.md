# Symphonia

웹 클라이언트 대상 Spring Boot API 서버. 베이스 패키지: `com.symphonia`

## Claude 협업 규칙 (필독)

- 요구사항이 애매하거나 기존 설계와 안 맞는 부분이 보이면 임의로 판단해서 진행하지 말고 **반드시 먼저 질문**한다.
- 설계가 여러 갈래로 갈리는 지점은 **DDD, 클린코드 원칙에 부합하는 방향**으로 제안하고 근거를 설명한다.
- 구현 전에 관련 스킬(`.claude/skills/<name>/SKILL.md`)을 먼저 확인한다.

## 하네스 설계 철학 (Agent Harness Design)

`.claude/` 설정은 단순 문서가 아니라 에이전트 행동을 프로그래밍하는 `실행 가능한 단일 진실 소스(harness)`다. 소스 코드처럼 계층화·단일 책임·리뷰 대상으로 다룬다. **CLAUDE.md 자신도 예외가 아니다. 이 파일은 라우팅 정보만 갖고, 판단 규칙의 본문은 전부 스킬에 둔다.**

**컴포넌트 → 계층형 아키텍처 매핑**

| 하네스 아티팩트 | 대응 | 책임 |
|---|---|---|
| `CLAUDE.md` | 매니페스트 | 뭐가 있고 언제 로드하는지만 담는다. 디테일은 절대 넣지 않는 작고 안정적인 라우터다 |
| `settings.json` | 보안/거버넌스 게이트 | 위험 명령 차단·승인 조건을 규칙으로 강제 (LLM 판단에 맡기지 않음) |
| `.claude/hooks/*` | 인터셉터 | 도구 호출 전후를 가로채 팀 컨벤션대로 행동을 교정. 현재 10개 작성됨: `block-main-commit.sh`(main 브랜치 직접 커밋 차단), `check-commit-issue-number.sh`(<type>/<이슈번호> 브랜치에서 커밋 메시지의 이슈 번호 불일치 차단), `check-commit-type-consistency.sh`(커밋 메시지 안에 서로 다른 type이 섞이면 차단), `check-develop-hotfix-commit.sh`(develop에서 [HotFix] 태그 없는 직접 커밋 차단), `check-no-claude-trailer.sh`(git commit/gh pr create,edit에 Claude 어트리뷰션 트레일러 포함 시 차단), `check-issue-todo-checked.sh`(gh pr create 전 연결된 이슈의 TODO 체크리스트에 미체크 항목이 있으면 차단), `pre-push-check.sh`(git push 전 check-all.sh 게이트), `post-edit-format.sh`(Java 파일 편집 후 Spotless 자동 포맷), `check-writing-style.sh`(응답 종료 시 글쓰기 스타일 검사), `stop-eof-newline.sh`(세션 종료 시 EOF 개행 자동 보정) |
| `.claude/commands/*` (`/new-domain`, `/new-feature`, `/run-checks`) | Controller | 사용자가 트리거하는 절차의 진입점 |
| 서브에이전트 (`domain-scaffolder`, `code-reviewer`, `test-*`) | Service | 격리된 컨텍스트에서 여러 단계를 조율 |
| `.claude/skills/<name>/SKILL.md` | SRP 컴포넌트 | 하나의 지식 영역 = 그 규칙의 단일 소스 |
| `.claude/scripts/*` (`check-all.sh`) | 결정론적 코어 | 절대 흔들리면 안 되는 컨벤션/검증을 담당한다. LLM 판단이 아니라 코드로 실행한다 |

**운영 원칙**

- **필요성 원칙 (progressive disclosure)**: 현재 작업에 필요한 컨텍스트만 로드한다. 아래 "작업별 로딩" 표가 그 구체적 시행 장치.
- **결정론적인 것은 스크립트, 판단이 필요한 것은 LLM**: 코드 스타일·EOF 개행처럼 완전히 결정론적인 것만 스크립트로 강제한다. 계층 의존 방향처럼 판단이 섞이는 규칙은 `code-reviewer` 에이전트가 체크리스트 기반으로 검토한다. (정적분석 도구 미도입 사유는 `architecture` 스킬 참고)
- **"예외 상황 → 질문"**: 되돌리기 어렵거나 대외적인 행동(삭제, 배포, force-push, 게시)은 확인 없이 임의로 진행하지 않는다.
- **하네스 아티팩트도 리뷰 대상**: 스킬·훅·커맨드도 소스코드처럼 피드백을 받아 고도화한다. 특정 스킬이 토큰을 과도하게 쓰거나 체크리스트를 잘못 적용하는 게 확인되면, 그 스킬 문서 자체가 수정 대상이다.

## 작업별 로딩 (Pre-Task)

상세 규칙은 **스킬**(`.claude/skills/<name>/SKILL.md`), 절차는 **커맨드**(`.claude/commands/`)에 있다. 아래 작업이 감지되면 스킬이 자동 로드된다 (수동 호출: `/<name>`).

| 작업 유형 | 로드 |
|---|---|
| 계층 설계, UseCase/Service/Api/Controller 분리, DTO↔Command 변환, 응답 포맷, 크로스 도메인 참조, 프로젝트 구조 | `architecture` |
| 네이밍, 포맷, 클린코드 스타일 | `code-style` |
| 테스트 작성 (JUnit5/Mockito/Testcontainers) | `testing` |
| 예외/에러 처리 | `error-handling` |
| Branch 전략, 전체 흐름 개요 | `git-workflow` |
| 커밋 전 검증, 커밋 단위, 커밋 메시지 포맷, push 규칙과 게이트 훅 동작 | `commit-push` |
| GitHub 이슈 생성 | `create-issue` |
| Pull Request 생성 | `create-pr` |
| 새 도메인 스캐폴딩 | `/new-domain` |
| 기존 도메인에 기능 추가 | `/new-feature` |
| PR 전 전체 검증 | `/run-checks` |

> 도메인 스캐폴딩은 `domain-scaffolder`, 코드 리뷰는 `code-reviewer`, 테스트 작성은 `test-writer`, 실패 테스트 원인 진단(수정 제외)은 `test-validator` 에이전트에 위임한다.
> `architecture`/`testing`/`git-workflow`/`commit-push`/`error-handling`/`create-issue`/`create-pr`/`code-style` 스킬은 작성 완료. `domain-scaffolder`/`code-reviewer`/`test-writer`/`test-validator` 에이전트와 `/new-domain`/`/new-feature`/`/run-checks` 커맨드, `check-all.sh`도 작성 완료.

## 핵심 제약 (Critical Constraints)

아래는 항상 적용되는 최소 체크리스트다. 근거와 상세 규칙은 괄호 안 스킬을 참고.

### ❌ 금지

- 코틀린 사용 (자바로만 개발하며 마이그레이션 계획 없음)
- 헥사고날 포트/어댑터 명칭, 멀티모듈 구조 (`architecture`)
- 도메인 로직을 Controller/Service에 절차적으로 나열 (`architecture`)
- `RuntimeException` 직접 throw (`error-handling`)
- `domain`이 `infrastructure` 패키지 참조 (`architecture`)
- `application`에서 `*Request`/`*Response` DTO 임포트 (`architecture`)
- 다른 도메인의 구현체·infrastructure·Repository 직접 참조 (`architecture`)
- `*Result`가 도메인 엔티티를 직접 감싸거나 그대로 노출 (`architecture`)

### ✅ 필수

- `*Service`는 `*UseCase` 구현체, `*Controller`는 `*Api` 구현체 (`architecture`)
- Repository·외부 연동도 인터페이스+구현체 분리 (`architecture`)
- 1 `*UseCase` = 1 `*Service` 기본 원칙, 크로스 도메인 조율은 대상 `*UseCase` 직접 의존 (Facade 없음) (`architecture`)
- 트랜잭션 경계는 `@CommandService`/`@QueryService` 클래스 단위 선언 (`architecture`)
- 테스트는 given-when-then, 메서드명 `should[기대동작]When[조건]` (`testing`)
- 커스텀 예외는 공통 베이스 예외 하위 + 에러코드로 관리 (`error-handling`)
- 커밋 메시지 `[#이슈번호] type: 설명` (`commit-push`)

## 기술 스택

| 영역 | 기술 |
|---|---|
| 언어/프레임워크 | Java / Spring Boot / Spring Security |
| 인증 | JWT 액세스 토큰(jjwt 0.12+, 응답 바디) + UUID 리프레시 토큰(Redis 저장, RTR 적용, httpOnly 쿠키 전달) |
| DB / Cache | JPA (Hibernate) / Redis |
| 테스트 | JUnit 5 / Mockito / Testcontainers |

## 프로젝트 구조 (단일 모듈, 도메인별 패키지)

```
com.symphonia
├── member/     {domain, application, presentation, infrastructure}
├── auth/       {domain, application, presentation, infrastructure}
├── common/     공통 예외, 응답 포맷, BaseTimeEntity, CQRS 트랜잭션 애노테이션 (공유 커널: domain/application도 참조)
└── global/     Security, JPA/Redis/Swagger 설정 등 기술 부트스트랩 (domain/application은 참조하지 않음)
```

> 계층 역할, 계층별 타입 어휘, 크로스 도메인 규칙 등 상세는 `architecture` 스킬 참고.

## Profiles

`local`/`dev`/`prod` 3개 환경별 프로파일. 공통 설정은 `application.yaml`, 환경별 값은 `application-{profile}.yaml`로 분리.

| 속성 성격 | 위치 |
|---|---|
| 환경 무관 동일 값 (`jpa.open-in-view`, `ddl-auto: validate`, dialect) | `application.yaml` |
| 환경별 값 (`datasource`, `data.redis`, `jwt`, `logging.level`) | `application-{profile}.yaml` |

- 활성 프로파일: `--spring.profiles.active={profile}` 또는 `SPRING_PROFILES_ACTIVE`
- **DB/Redis 연결 정보는 값이 같아도 프로파일별로 각각 명시** (환경 간 독립적 분기). 비밀값은 하드코딩 금지, `${ENV_VAR}` 주입 (`.env` 커밋 금지)

## 명령어

```bash
./gradlew test                     # 전체 테스트 (빠른 반복 확인용)
./gradlew build                    # 빌드 (컴파일+테스트+패키징, 로컬 검증용)
./.claude/scripts/check-all.sh     # PR 전 전체 검증 (포맷 검증 포함)
```

> `check-all.sh`는 내부적으로 `./gradlew clean build`를 실행한다. 증분 캐시로 인한 "로컬 통과, CI 실패" stale 상태를 놓치지 않기 위해서다.
> 포맷터는 Spotless + Google Java Format으로 확정했다(2026-09-05, `code-style` 스킬 참고). `spotlessCheck`가 `check` 태스크에 연결되어 있어 `clean build`에 자동으로 포함된다. `.claude/hooks/post-edit-format.sh`(PostToolUse)가 Java 파일 편집 시마다 자동으로 포맷을 적용한다.
> 아키텍처 규칙 자동검증(ArchUnit 등) 미도입 사유는 위 "운영 원칙" 참고.
