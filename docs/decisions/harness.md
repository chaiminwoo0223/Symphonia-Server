# 하네스 결정 기록

## 평가 시나리오 (2026-11-08, 이슈 #178)

하네스를 바꾼 뒤 토큰 수, 소요 시간, 검증 결과가 어떻게 달라졌는지 같은 조건으로 비교하려고 시나리오 3개를 고정한다. 하네스를 다시 고칠 때도 이 시나리오로 측정한다.

### 실행 방법

1. 시나리오의 기준 커밋으로 임시 worktree를 만든다. `git worktree add ../symphonia-eval <기준 커밋>`
2. 변경 후 하네스를 측정할 때는 worktree에서 `git checkout <측정할 브랜치> -- .claude CLAUDE.md docs/decisions`로 하네스만 덮어쓴다. 코드는 기준 커밋 그대로 둔다.
3. worktree에서 새 Claude Code 세션을 열고 시나리오 프롬프트를 그대로 입력한다. 중간 질문에는 추천안을 고른다.
4. 작업이 끝나면 `/cost`의 토큰 수, API 시간, 전체 시간과 검증 결과를 기록한다.
5. 측정이 끝나면 `git worktree remove ../symphonia-eval --force`로 지운다. 결과물은 커밋하지 않는다.

### 시나리오

| 이름 | 기준 커밋 | 준비 | 프롬프트 | 검증 |
|---|---|---|---|---|
| 기능 추가 | `09dfde8` | 없음 | `catalog에 id 목록으로 음료, 안주, 음악 무드의 이름과 음악 무드 스트리밍 URL을 조회하는 GetCatalogReferencesUseCase를 추가해줘. CatalogQueryService가 함께 구현하고, 없는 대상은 기존 NotFound 예외로 알려줘. 테스트도 작성해줘.` | `./gradlew test --tests '*CatalogQueryServiceTest'` 통과, `check-test-conventions.sh` 통과 |
| 테스트 작성 | `53b2bd4` | `git checkout 09dfde8 -- src/test/java/com/symphonia/catalog/application/service/CatalogQueryServiceTest.java` | `test-writer 에이전트로 CatalogQueryService의 GetCatalogReferencesUseCase 구현에 대한 테스트를 작성해줘.` | `./gradlew test --tests '*CatalogQueryServiceTest'` 통과, `check-test-conventions.sh` 통과 |
| 코드 리뷰 | `6e2109c` | 없음 | `git diff 09dfde8 6e2109c 전체를 code-reviewer 에이전트에 넘겨 리뷰해줘. 수정은 하지 마.` | CRITICAL, MAJOR 지적 수와 내용 |

## 결정 기록 위치

`design-decision` 스킬에 있던 결정 문단을 원문 그대로 옮겼다(2026-11-08, 이슈 #178).

**(2026-10-25 결정, 이슈 #134)**: 위 목록에서 `architecture/<도메인>.md`에 해당하는 결정은 새 도메인(예: #122의 `gathering` 정산 규칙)이라도 처음부터 그 하위 파일에 적는다. 그 도메인의 카탈로그 적재나 DB 결정은 `persistence.md`에 적는다. 한 결정이 도메인 규칙과 영속성 기술을 함께 다루면 결정의 이유가 속한 쪽에 적는다. 예를 들어 `drink.harmonizes` 배열 컬럼(#105)은 추천 성능 때문에 정했으므로 `pairing.md`에 있다. `persistence.md`와 `pairing.md`는 기존 절을 그대로 옮겨 만들었으므로, 이 기준과 다른 위치에 있는 앞 결정(`persistence.md`의 #75 애그리거트 분리)과 다른 파일 작업에도 필요한 결정은 `SKILL.md`의 읽는 시점 안내에 적는다. 지금 하위 파일이 있는 스킬은 결정이 가장 많이 쌓이는 `architecture`뿐이다. 다른 스킬도 한 절이 그 `SKILL.md` 나머지 본문의 바이트 수보다 커지면 같은 방식으로 하위 파일로 옮긴다. 하위 파일을 만들면 `SKILL.md`에 그 주제의 절을 두고 그 파일을 언제 읽는지 한 문단으로 적는다. `description`에도 그 주제를 추가해 자동 로드 판단에 쓰이게 한다. 하위 파일은 그 주제의 작업을 할 때만 읽혀서, 그 결정이 필요 없는 작업이 결정 이력 전체를 함께 로드하지 않는다. `architecture`가 67KB까지 커졌을 때 72%가 영속성과 pairing 결정이었고, 계층 설계 작업마다 이 이력 전체가 로드됐다. 도메인마다 별도 스킬을 만드는 안은 택하지 않았다. 계층 규칙과 도메인 결정이 서로를 참조하는데 스킬 목록과 자동 로드 설명만 늘어나기 때문이다.

**(2026-11-08 결정, 이슈 #178)**: 결정을 스킬 인라인에 두던 방식을 바꿔, 스킬에는 현재 규칙만 명령형으로 두고 근거, 택하지 않은 안, 이력은 `docs/decisions/<주제>.md`로 옮긴다(#134에서 변경). 스킬 본문의 상당 부분이 근거와 이력이라 작업마다 수만 자가 로드됐고, 토큰 부족으로 세션이 자주 끊겼기 때문이다. 결정 로그가 스킬과 어긋나기 시작해도 아무도 갱신하지 않는다는 앞 결정의 우려는, 날짜순 로그가 아니라 스킬과 같은 주제 단위로 파일을 나누고 규칙과 근거를 같은 커밋에서 고치는 규칙으로 줄인다. `commit-push`, `git-workflow`, `create-*`, `code-style`의 인라인 결정은 이번 범위에서 옮기지 않았다.

## 결정론적 검사와 검증 속도 (2026-11-08, 이슈 #178)

**(2026-11-08 결정, 이슈 #178)**: `check-all.sh`는 로컬에서 성공하면 한 줄만, 실패하면 실패한 단계, 실패한 테스트와 예외 줄, Spring 애플리케이션 로그를 뺀 로그 끝부분만 출력한다. 전체 로그는 `clean`이 지우지 않는 `.git/check-all.log`에 남긴다. 성공 출력이 수백 줄이면 에이전트가 검증할 때마다 그만큼 토큰을 쓰기 때문이다. `CI` 환경변수가 있으면 전체 로그를 그대로 출력한다. 사람이 Actions 화면에서 원인을 찾아야 하기 때문이다. 처음에는 로그 끝 40줄을 그대로 보여 줬는데, 테스트 실패에서도 끝부분이 컨텍스트 종료 로그로만 채워져 타임스탬프로 시작하는 줄을 걸렀다.

**(2026-11-08 결정, 이슈 #178)**: 작업 트리가 깨끗한 상태로 `check-all.sh`를 통과하면 HEAD를 `.git/check-all-passed`에 기록하고, `pre-push-check.sh`는 기록과 HEAD가 같고 작업 트리가 깨끗하면 검증을 건너뛴다. PR 전 `/run-checks`와 push 게이트가 같은 커밋을 두 번 검증했기 때문이다. 검증 시작과 끝에 HEAD와 작업 트리를 모두 확인하므로, 검증 중 바뀐 상태를 통과로 기록하지 않는다.

**(2026-11-08 결정, 이슈 #178)**: 분량 예산은 `CLAUDE.md` 100줄과 5,000자, `SKILL.md` 12,000자, 스킬 하위 문서 10,000자다. 스킬 파일은 한 줄이 수백 자라(`gathering.md`는 53줄에 45,217바이트) 줄 수로는 분량을 잴 수 없다. 처음 제안한 `SKILL.md` 10,000자는 규칙만 남긴 `architecture/SKILL.md`가 11,163자라 12,000자로 정했다. 문자 수는 UTF-8 이어지는 바이트를 뺀 바이트 수로 세어 macOS와 CI의 로케일 차이를 없앴다.

**(2026-11-08 결정, 이슈 #178)**: `test-validator`와 `domain-scaffolder`는 `model: sonnet`을 지정한다. 진단 절차와 골격 생성이 정해져 있어 깊은 판단이 필요 없고, 응답이 빠르기 때문이다. `code-reviewer`는 판단형 항목만 보도록 체크리스트를 줄이되 모델은 바꾸지 않았다. 리뷰 품질을 낮추지 않기 위해서다.

**(2026-11-08 확인, 이슈 #178)**: 프로젝트 설정으로 끌 MCP 서버는 없다. 저장소에 `.mcp.json`이 없고, 켜져 있는 MCP는 사용자 수준의 claude.ai 커넥터와 Chrome 확장이다. 이 도구들은 세션에서 deferred 상태라 컨텍스트에는 이름만 실려 끄더라도 줄어드는 토큰이 작다. 프로젝트 `.mcp.json`을 추가하게 되면 쓰지 않는 서버를 `disabledMcpjsonServers`로 끈다.
