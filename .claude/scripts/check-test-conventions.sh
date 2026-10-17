#!/usr/bin/env bash
# 테스트 컨벤션 중 결정론적으로 판정할 수 있는 항목만 검사한다(testing 스킬 참고).
# 실행: ./.claude/scripts/check-test-conventions.sh [--all]
#
# 검사 대상은 origin/develop과 갈라진 지점 이후 바뀐 src/test의 *Test.java다. 커밋하지 않은 변경과
# 새 파일도 포함한다. --all을 주거나 origin/develop이 없으면 src/test 전체를 검사한다.
# 블록 안의 내용이 맞는지(when의 SUT 단일 호출 등)는 판단이 필요해 code-reviewer가 검토한다.

set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

AWK_SCRIPT="$ROOT/.claude/scripts/check-test-conventions.awk"
BASE_REF="origin/develop"

scan_all=0
[[ "${1:-}" == "--all" ]] && scan_all=1

if [[ $scan_all -eq 0 ]] && ! base="$(git merge-base "$BASE_REF" HEAD 2>/dev/null)"; then
    echo "⚠ $BASE_REF를 찾지 못해 src/test 전체를 검사한다."
    scan_all=1
fi

if [[ $scan_all -eq 1 ]]; then
    files="$(find src/test -name '*Test.java' | sort)"
else
    files="$(
        {
            git diff --name-only --diff-filter=d "$base" -- 'src/test/*Test.java'
            git ls-files --others --exclude-standard -- 'src/test/*Test.java'
        } | sort -u
    )"
fi

if [[ -z "$files" ]]; then
    echo "✓ 검사할 테스트 파일이 없다."
    exit 0
fi

file_count="$(wc -l <<<"$files" | tr -d ' ')"

# 파일명에 공백이 없다는 전제로 단어 분리를 그대로 쓴다.
# shellcheck disable=SC2086
if ! awk -f "$AWK_SCRIPT" $files; then
    echo ""
    echo "✗ 테스트 컨벤션 위반이 있다. 위 메서드를 testing 스킬의 given-when-then, 명명 규칙에 맞게 고쳐라."
    exit 1
fi

echo "✓ 테스트 파일 ${file_count}개가 컨벤션 검사를 통과했다."
