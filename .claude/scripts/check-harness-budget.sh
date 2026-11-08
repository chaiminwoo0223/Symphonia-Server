#!/usr/bin/env bash
# CLAUDE.md와 스킬 문서가 분량 예산을 넘지 않는지 검사한다(harness 스킬 참고).
# 실행: ./.claude/scripts/check-harness-budget.sh
#
# CLAUDE.md는 매 턴, SKILL.md는 작업마다, 스킬 하위 문서는 그 주제 작업마다 로드되므로 따로 한도를 둔다.
# 스킬 파일은 한 줄이 수백 자라 줄 수로는 분량을 잴 수 없어 문자 수로 잰다. CLAUDE.md만 줄 수도 함께 본다.
# 문자 수는 UTF-8 이어지는 바이트(0x80~0xBF)를 뺀 바이트 수다. 로케일과 OS에 관계없이 같은 값이 나온다.

set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

CLAUDE_MD_MAX_LINES=100
CLAUDE_MD_MAX_CHARS=5000
SKILL_MAX_CHARS=12000
SKILL_SUB_MAX_CHARS=10000

violations=0

char_count() {
    LC_ALL=C tr -d '\200-\277' <"$1" | wc -c | tr -d ' '
}

check_chars() {
    local file="$1" max="$2" chars
    chars="$(char_count "$file")"
    if [[ $chars -gt $max ]]; then
        echo "✗ $file: ${chars}자 (한도 ${max}자)"
        violations=$((violations + 1))
    fi
}

lines="$(wc -l <CLAUDE.md | tr -d ' ')"
if [[ $lines -gt $CLAUDE_MD_MAX_LINES ]]; then
    echo "✗ CLAUDE.md: ${lines}줄 (한도 ${CLAUDE_MD_MAX_LINES}줄)"
    violations=$((violations + 1))
fi
check_chars CLAUDE.md "$CLAUDE_MD_MAX_CHARS"

while IFS= read -r file; do
    if [[ "$(basename "$file")" == "SKILL.md" ]]; then
        check_chars "$file" "$SKILL_MAX_CHARS"
    else
        check_chars "$file" "$SKILL_SUB_MAX_CHARS"
    fi
done < <(find .claude/skills -name '*.md' | sort)

if [[ $violations -gt 0 ]]; then
    echo ""
    echo "✗ 분량 예산 초과 ${violations}건. 근거, 택하지 않은 안, 이력은 docs/decisions/로 옮기고 규칙만 남긴다(design-decision 스킬)."
    echo "  → 그래도 넘으면 주제를 하위 문서로 나눈다. 한도를 올리기 전에 사용자에게 먼저 묻는다."
    exit 1
fi

echo "✓ 하네스 분량 예산 검사를 통과했다."
