---
name: create-pr
description: Load when creating a pull request. PR 대상/제목/본문 구성 규칙과 gh pr create 절차.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Pull Request 생성

`.github/PULL_REQUEST_TEMPLATE.md`의 구조를 그대로 따라 PR을 생성한다. 브랜치 전략은 `git-workflow` 스킬, 커밋 컨벤션은 `commit-push` 스킬을 참고한다.

## 규칙

- PR 대상은 항상 `develop`이다.
- PR 본문에 Claude 어트리뷰션 트레일러(`Co-Authored-By: Claude ...`, `Claude-Session: ...`)를 붙이지 않는다(`commit-push` 스킬 참고). `.claude/hooks/check-no-claude-trailer.sh`가 `gh pr create`/`gh pr edit`에 섞이면 차단한다.
- 현재 브랜치(`<type>/<이슈번호>`)에 연결된 이슈의 TODO 체크리스트는 `gh pr create` 전에 전부 체크(`- [x]`)해 둔다. `.claude/hooks/check-issue-todo-checked.sh`가 체크 안 된 항목이 남아 있으면 `gh pr create` 자체를 차단한다. 이번 PR 범위에서 뺀 TODO가 있다면 체크하지 말고 이슈 본문에서 별도 이슈로 옮기거나 범위 제외로 명시한 뒤 진행한다.
- 제목 포맷은 `[Type] 설명`이다. 이슈 번호는 제목에 넣지 않는다. 이슈 연결은 본문의 `closes #이슈번호`만으로 한다. `Type`은 `create-issue` 스킬의 타입 표(Feature/Bug/Refactor/Chore/Performance/Docs)를 그대로 쓴다. 여러 유형이 섞였으면 가장 비중이 큰 변경의 타입을 대표로 쓴다.
- 본문은 `.github/PULL_REQUEST_TEMPLATE.md` 구조(PR 유형 체크박스 → 작업 내용 → 관련 이슈 → 추가 사항)를 그대로 따른다. 구분선(`---`)과 섹션 순서를 임의로 바꾸지 않는다.
- **assignee는 항상 `--assignee @me`로 지정한다.** PR을 생성하는 사람이 곧 작업자이므로 별도 판단 없이 고정값으로 둔다.
- **label은 제목의 대표 `Type`에 대응하는 라벨을 `--label`로 지정한다.** 매핑은 `create-issue` 스킬의 타입→라벨 표(`✨ feature`/`🐛 bug`/`♻️ refactor`/`⚙️ chore`/`⚡️ performance`/`📄 docs`)를 그대로 재사용한다. 이슈와 마찬가지로 PR도 라벨 하나로 대표 타입만 표시하고, 위 title의 대표 타입과 항상 같은 라벨을 쓴다.

## 절차

1. PR 유형 체크박스에서 해당하는 항목 전부에 체크한다. 새로운 기능 추가, 리팩토링, 버그 수정, 설정, 문서화, 테스트 코드 추가, 기타 변경 사항 중 여러 개가 동시에 해당할 수 있다.
2. 작업 내용을 작성한다. 기능·모듈 단위로 이모지 소제목을 나누고, 그 아래 구체적인 클래스·메서드 이름과 함께 변경 사항을 나열한다.
3. 관련 이슈를 `closes #이슈번호`로 명시한다.
4. 필요하면 추가 사항에 리뷰어가 알아야 할 맥락을 적는다.
5. 제목의 대표 `Type`에 대응하는 라벨을 정한다 (예: `Docs` → `📄 docs`).
6. 생성한다. `--assignee @me`와 5번에서 정한 `--label`을 빠뜨리지 않는다.
   ```bash
   gh pr create --base develop --title "[Type] 설명" --assignee @me --label "라벨" --body "$(cat <<'EOF'
   ## ✅ PR 유형
   어떤 변경 사항이 있었나요?

   - [ ] 새로운 기능 추가
   - [ ] 리팩토링
   - [ ] 버그 수정
   - [ ] 설정
   - [ ] 문서화
   - [ ] 테스트 코드 추가
   - [ ] 기타 변경 사항 (주석, 개행 등)

   ---

   ## ✏️ 작업 내용


   ---

   ## 🔗 관련 이슈
   - closes #이슈번호

   ---

   ## 💡 추가 사항
   EOF
   )"
   ```

## 예시

실제 발급된 Performance 타입 PR이다. 작업 내용을 이모지 소제목으로 나누고, 그 아래 중첩 불릿으로 세부 변경을 적고, 개선 효과는 표로 제시하는 형태를 그대로 따른다.

```markdown
[Performance] 상대방 사주 목록 조회 N+1 쿼리 개선

## ✅ PR 유형
어떤 변경 사항이 있었나요?

- [ ] 새로운 기능 추가
- [x] 리팩토링
- [ ] 버그 수정
- [ ] 설정
- [ ] 문서화
- [x] 테스트 코드 추가
- [ ] 기타 변경 사항 (주석, 개행 등)

---

## ✏️ 작업 내용
### 🔀 헤더 배치(IN) 조회로 전환
  - GetPartnerSajusService.getPartners
    - 링크마다 sajuChartRepository.findById를 반복 호출하던 루프를 findSummariesByIds 배치 조회 1번으로 전환
    - 링크 목록에서 chartId를 모아 한 번에 조회한 뒤 associateBy로 매칭, 순서는 링크 순서를 그대로 유지
  - SajuChartRepository.findSummariesByIds(ids) 포트 신설
  - SajuChartRepositoryImpl은 chartJpaRepository.findAllById만 사용
  - SajuChartSummary 신설
    - PartnerSajuSummary가 실제로 사용하는 필드(name/gender/calendarType/inputDate/birthTime/isTimeUnknown)만 담음
    - 목록에 노출되지 않는 4주·오행·십성 상세 조회를 배치 경로에서 제거
  - SajuChartJpaEntity.toSummary() 추가

### 🧪 테스트
  - GetPartnerSajusServiceTest 단위 테스트 추가
  - SajuChartRepositoryImplTest에 findSummariesByIds 케이스 추가
  - SajuFixture에 chartSummary() 헬퍼 추가

### 📊 성능 측정 (로컬, TestContainer 기반 실제 Postgres 사용)
| 파트너 수 | 지표 | 개선 전 | 개선 후 | 개선 |
|---|---|---|---|---|
| 10명 | 쿼리 수 | 30회 | 1회 | 30배 |
| 10명 | 소요 시간 | 448ms | 11ms | 약 40배 |

---

## 🔗 관련 이슈
- closes #85

---

## 💡 추가 사항
```

**스타일 규칙**

- 작업 내용은 기능 단위로 이모지 소제목(예: 🔀 전환, 🧪 테스트, 📊 측정)을 나누고, 그 아래 클래스·메서드 이름을 구체적으로 언급하는 중첩 불릿으로 적는다.
- 정량적 효과가 있는 변경(성능 개선 등)은 개선 전/후를 표로 제시한다.
- 추상적인 요약 대신 실제로 어떤 메서드·클래스가 왜 바뀌었는지 풀어서 적는다.

## 원칙

- 템플릿 구조(섹션 순서, 구분선)를 임의로 바꾸지 않는다.
- 본문은 한국어로 작성한다.
- `closes #이슈번호`를 빠뜨리지 않는다. 이슈가 자동으로 닫히는 유일한 연결 고리다.
