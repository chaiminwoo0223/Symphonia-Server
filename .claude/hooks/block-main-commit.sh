#!/usr/bin/env bash
# PreToolUse(Bash) 훅: `main` 브랜치에서의 직접 커밋을 차단한다.
#
# main은 운영 배포 브랜치라 로컬에서 직접 커밋하는 변경이 있으면 안 된다(2026-09-05).
# develop은 [HotFix] 커밋으로 직접 해결하는 경우가 있으므로 이 훅의 대상이 아니다.
# 브랜치 판별에 실패하는 상황(git 저장소 밖 등)에서는 항상 통과시킨다(fail-open).

payload="$(cat)"
command="$(jq -r '.tool_input.command // empty' <<<"$payload" 2>/dev/null)"

if [[ ! "$command" =~ (^|[[:space:]\&\|\;])git[[:space:]]+commit ]]; then
    exit 0
fi

ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || echo .)}"
cd "$ROOT" || exit 0

branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null)"
[[ "$branch" == "main" ]] || exit 0

reason="main 브랜치는 직접 커밋할 수 없다. develop에서 분기한 feat/#이슈번호 브랜치에서 작업한 뒤 PR로 병합해라."

jq -n --arg reason "$reason" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
exit 0
