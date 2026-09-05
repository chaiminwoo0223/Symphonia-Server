#!/usr/bin/env bash
# PreToolUse(Bash) 훅: `git push` 직전 전체 빌드+테스트를 강제한다.
#
# check-all.sh(./gradlew clean build)로 게이트를 건다.
# push마다 수 분이 걸릴 수 있음을 감수하기로 했다(2026-09-04).
# 실패해도 push를 막지 못하는 상황(스크립트 오류 등)에서는 항상 통과시킨다(fail-open).

payload="$(cat)"
command="$(jq -r '.tool_input.command // empty' <<<"$payload" 2>/dev/null)"

if [[ ! "$command" =~ (^|[[:space:]\&\|\;])git[[:space:]]+push ]]; then
    exit 0
fi

ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || echo .)}"
cd "$ROOT" || exit 0

[[ -x "./.claude/scripts/check-all.sh" ]] || exit 0

if output="$(./.claude/scripts/check-all.sh 2>&1)"; then
    exit 0
fi

reason="git push 전 전체 검증(check-all.sh)이 실패했다. 문제를 해결한 뒤 다시 push해라.
$(tail -30 <<<"$output")"

jq -n --arg reason "$reason" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
exit 0
