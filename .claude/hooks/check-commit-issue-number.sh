#!/usr/bin/env bash
# PreToolUse(Bash) 훅: git commit 메시지의 이슈 번호가 현재 브랜치(feat/<이슈번호>)와 일치하는지 검증한다.
#
# 하나의 브랜치·PR·이슈 번호로 관리한다(2026-09-05, 사용자 요청).
# feat/<번호> 패턴이 아닌 브랜치(main, develop 등)는 강제할 기준 자체가 없으므로 통과시킨다.
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
[[ "$branch" =~ ^feat/([0-9]+)$ ]] || exit 0
branch_issue="${BASH_REMATCH[1]}"

msg_issue="$(grep -oE '\[#[0-9]+\]' <<<"$command" | head -1 | grep -oE '[0-9]+')"

if [[ -z "$msg_issue" ]]; then
    reason="커밋 메시지에 [#이슈번호]가 없다. 현재 브랜치(${branch})는 이슈 #${branch_issue}에 연결되어 있으니 [#${branch_issue}] type: 설명 형식으로 커밋해라."
    jq -n --arg reason "$reason" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
    exit 0
fi

if [[ "$msg_issue" != "$branch_issue" ]]; then
    reason="커밋 메시지의 이슈 번호(#${msg_issue})가 현재 브랜치(${branch})의 이슈 번호(#${branch_issue})와 다르다. 브랜치·PR·이슈 번호는 하나로 맞춰야 하니 [#${branch_issue}]로 커밋해라."
    jq -n --arg reason "$reason" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
    exit 0
fi

exit 0
