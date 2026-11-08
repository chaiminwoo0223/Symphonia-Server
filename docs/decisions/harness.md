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
