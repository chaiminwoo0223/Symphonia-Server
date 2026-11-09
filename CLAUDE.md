# Symphonia

- 술과 안주를 자리의 분위기에 맞게 조합해 추천하는 페어링 서비스이고, 술자리 정산 기능을 함께 제공한다.
- 웹 클라이언트를 대상으로 하는 Spring Boot API 서버로 제공된다.
- 베이스 패키지는 `com.symphonia`이다.

## Claude 협업 규칙 (필독)

- 요구사항이 애매하거나 기존 설계와 안 맞는 부분이 보이면 임의로 판단해서 진행하지 말고 **반드시 먼저 질문**한다.
- 설계가 여러 갈래로 갈리는 지점은 **DDD, 클린코드 원칙에 부합하는 방향**으로 제안하고 근거를 설명한다.
- 구현 전에 관련 스킬(`.claude/skills/<name>/SKILL.md`)을 먼저 확인한다.
- 되돌리기 어렵거나 대외적인 행동(삭제, 배포, force-push, 게시)은 확인 없이 진행하지 않는다.
- 이 파일은 라우팅 정보만 갖는다. 규칙의 본문은 스킬에, 근거와 이력은 `docs/decisions/`에 둔다.

## 작업별 로딩 (Pre-Task)

아래 작업이 감지되면 해당 스킬을 로드한다(수동 호출: `/<name>`). `/`로 표기한 절차 스킬은 사용자만 호출할 수 있다.

| 작업 유형 | 로드 |
|---|---|
| 계층 설계, UseCase/Service/Api/Controller 분리, DTO↔Command 변환, 응답 포맷, 크로스 도메인 참조, 인증, 영속성, 도메인별 규칙 | `architecture` |
| 네이밍, 포맷, 멤버 선언 순서, 클린코드 스타일 | `code-style` |
| 테스트 작성 (JUnit5/Mockito/Testcontainers) | `testing` |
| 예외/에러 처리 | `error-handling` |
| Branch 전략, 전체 흐름 개요, CodeRabbit 역할 분담 | `git-workflow` |
| 커밋 전 검증, 커밋 단위, 커밋 메시지 포맷, push 규칙과 게이트 훅 | `commit-push` |
| GitHub 이슈 생성 | `create-issue` |
| Pull Request 생성 | `create-pr` |
| 설계 논의가 하나로 결론 났을 때 기록 위치와 형식 | `design-decision` |
| `CLAUDE.md`, `settings.json`, 훅, 스크립트, 에이전트, 스킬 수정 | `harness` |
| 새 도메인 스캐폴딩 | `/new-domain` |
| 기존 도메인에 기능 추가 | `/new-feature` |
| PR 전 전체 검증 | `/run-checks` |

> 도메인 스캐폴딩은 `domain-scaffolder`, 코드 리뷰는 `code-reviewer`, 테스트 작성은 `test-writer`, 실패 테스트 원인 진단(수정 제외)은 `test-validator` 에이전트에 위임한다.

## 핵심 제약 (Critical Constraints)

항상 적용되는 최소 체크리스트다. 괄호는 상세 규칙이 있는 스킬이다. ✂ 표시는 `check-architecture.sh`가 검사한다.

### ❌ 금지

- 코틀린 사용 ✂
- 헥사고날 포트/어댑터 명칭 ✂, 멀티모듈 구조 (`architecture`)
- 도메인 로직을 Controller/Service에 절차적으로 나열 (`architecture`)
- `RuntimeException` 직접 throw ✂ (`error-handling`)
- `domain`이 `infrastructure` 패키지 참조 ✂ (`architecture`)
- `application`에서 `*Request`/`*Response` DTO 임포트 ✂ (`architecture`)
- 다른 도메인의 구현체·infrastructure·Repository 직접 참조 ✂ (`architecture`)
- 다른 도메인의 `domain` 패키지에서 enum 외의 타입 참조 ✂, 다른 도메인 enum이 선언한 메서드 호출 (`architecture`)
- `*Result`가 도메인 엔티티를 직접 감싸거나 그대로 노출 (`architecture`)

### ✅ 필수

- `*Service`는 `*UseCase` 구현체, `*Controller`는 `*Api` 구현체 (`architecture`)
- Repository·외부 연동도 인터페이스+구현체 분리 (`architecture`)
- 1 `*UseCase` = 1 `*Service` 기본 원칙, 크로스 도메인 조율은 대상 `*UseCase` 직접 의존 (Facade 없음) (`architecture`)
- 트랜잭션 경계는 `@CommandService`/`@QueryService` 클래스 단위 선언, 메서드 단위 `@Transactional` 금지 ✂ (`architecture`)
- 테스트는 given-when-then, 메서드명 `should[기대동작]When[조건]` (`testing`)
- 커스텀 예외는 공통 베이스 예외 하위 + 에러코드로 관리 (`error-handling`)
- 커밋 메시지 `[#이슈번호] type: 설명` (`commit-push`)

## 기술 스택

| 영역 | 기술 |
|---|---|
| 언어/프레임워크 | Java / Spring Boot / Spring Security |
| 인증 | JWT 액세스 토큰(jjwt 0.12+, 응답 바디) + UUID 리프레시 토큰(Redis 저장, RTR 적용, httpOnly 쿠키 전달) |
| DB / Cache | PostgreSQL 17 (`pgvector/pgvector:pg17` 이미지) + JPA (Hibernate) / Redis |
| 메시지 큐 | RabbitMQ 4.3 (정산 메시지 발송) |
| 테스트 | JUnit 5 / Mockito / Testcontainers |

## 프로젝트 구조 (단일 모듈, 도메인별 패키지)

`com.symphonia` 아래 도메인 `member`, `auth`, `catalog`, `pairing`, `gathering`, `notification`이 각각 `domain`, `application`, `presentation`, `infrastructure`를 갖는다. `common`은 공유 커널(공통 예외, 응답 포맷, `BaseTimeEntity`, CQRS 트랜잭션 애노테이션)이고, `global`은 기술 부트스트랩(Security, JPA/Redis/Swagger 설정)이다. 도메인별 책임은 `architecture` 스킬에 있다.

## Profiles

- `local`/`dev`/`prod` 3개 프로파일이다. 공통 설정은 `application.yaml`, 환경별 값은 `application-{profile}.yaml`에 둔다. 지정하지 않으면 `local`이고, dev와 prod는 `SPRING_PROFILES_ACTIVE`로 반드시 지정한다.
- DB/Redis 연결 정보는 값이 같아도 프로파일별로 각각 명시한다. 비밀값은 `${ENV_VAR}`로 주입하고 `.env`는 커밋하지 않는다.
- 스키마와 시드 데이터는 Flyway 마이그레이션(`src/main/resources/db/migration`)으로만 바꾼다. 상세는 `architecture` 스킬의 `persistence.md`에 있다.

## 명령어

```bash
./gradlew test                     # 전체 테스트 (빠른 반복 확인용)
./.claude/scripts/check-all.sh     # PR 전 전체 검증. 성공하면 한 줄만 출력하고, 전체 로그는 Git 디렉터리의 check-all.log에 남긴다. CI에서는 전체 로그를 출력한다
./.claude/scripts/check-architecture.sh      # import와 선언으로 판정하는 계층 규칙 검사
./.claude/scripts/check-test-conventions.sh  # 바뀐 테스트의 given-when-then 블록과 should 접두어 검사 (--all: 전체)
./.claude/scripts/check-harness-budget.sh    # 스킬과 CLAUDE.md의 분량 예산 검사
./scripts/import/fetch-bjcp.sh     # BJCP 원본 JSON 다운로드 (적재 전 1회, 원본은 커밋하지 않음)
./gradlew importCatalog            # 음료, 안주 카탈로그 적재 (배포와 분리한 수동 실행)
```

> `check-all.sh`는 테스트 컨벤션, 계층 규칙, 분량 예산 검사 뒤 `./gradlew clean build`를 실행한다. `clean`은 증분 캐시로 인한 "로컬 통과, CI 실패"를 막는다. CI(`.github/workflows/ci.yml`)도 같은 스크립트를 실행한다.
> 포맷터는 Spotless + Google Java Format이고, `post-edit-format.sh` 훅이 Java 파일을 편집할 때마다 포맷을 적용한다(`code-style` 스킬).
