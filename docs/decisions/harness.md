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
