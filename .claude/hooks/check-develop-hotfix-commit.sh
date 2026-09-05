#!/usr/bin/env bash
# PreToolUse(Bash) 훅: develop 브랜치는 긴급 [HotFix] 커밋만 직접 허용한다.
#
# develop은 PR 병합으로 통합하는 브랜치라 직접 커밋은 긴급 대응(hotfix)에 한정한다(2026-09-05, 사용자 요청).
# [HotFix] 커밋은 이슈 번호 없이 [HotFix] type: 설명 형식을 쓴다.
# develop이 아닌 브랜치는 강제할 기준이 없으므로 통과시킨다.
# -m 없이 에디터로 메시지를 작성하는 커밋(예: --amend --no-edit)은 최종 메시지를 알 수 없으므로 검증하지 않고 통과시킨다(fail-open).

payload="$(cat)"
command="$(jq -r '.tool_input.command // empty' <<<"$payload" 2>/dev/null)"

if [[ ! "$command" =~ (^|[[:space:]\&\|\;])git[[:space:]]+commit ]]; then
    exit 0
fi

[[ "$command" =~ (^|[[:space:]])-m([[:space:]]|=) ]] || exit 0

ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || echo .)}"
cd "$ROOT" || exit 0

branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null)"
[[ "$branch" == "develop" ]] || exit 0

if [[ ! "$command" =~ \[HotFix\] ]]; then
    reason="develop 브랜치는 긴급 [HotFix] 커밋만 직접 허용한다. 일반 기능/수정 작업은 feat/<이슈번호> 브랜치를 만들어 PR로 병합해라. 긴급 수정이면 [HotFix] type: 설명 형식으로 커밋해라 (이슈 번호 없음)."
    jq -n --arg reason "$reason" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
    exit 0
fi

exit 0
