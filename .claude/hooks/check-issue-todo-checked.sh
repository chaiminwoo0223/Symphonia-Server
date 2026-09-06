#!/usr/bin/env bash
# PreToolUse(Bash) 훅: gh pr create 실행 전, 현재 브랜치(<type>/<이슈번호>)에 연결된 이슈의
# TODO 체크리스트가 전부 체크됐는지 검증한다.
#
# 배경(이슈 #23): "작업이 실제로 다 끝났는지"는 이슈를 체크할 때 이미 LLM이 판단해서 끝내둔 것이므로, 여기서는 그 판단 결과인 체크 여부(- [ ] vs - [x])만 문자열로 확인하는 결정론적 게이트다.
# 판단 자체를 훅이 다시 하지 않는다.
# <type>/<이슈번호> 패턴이 아닌 브랜치(main, develop 등)는 강제할 기준이 없으므로 통과시킨다.
# gh 인증/네트워크 문제로 이슈를 못 읽으면 PR 생성 자체를 막지 않는다(fail-open).

payload="$(cat)"
command="$(jq -r '.tool_input.command // empty' <<<"$payload" 2>/dev/null)"

[[ "$command" =~ (^|[[:space:]\&\|\;])gh[[:space:]]+pr[[:space:]]+create ]] || exit 0

ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || echo .)}"
cd "$ROOT" || exit 0

branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null)"
[[ "$branch" =~ ^[a-z]+/([0-9]+)$ ]] || exit 0
issue_number="${BASH_REMATCH[1]}"

body="$(gh issue view "$issue_number" --json body -q .body 2>/dev/null)"
[[ -z "$body" ]] && exit 0

unchecked="$(grep -cE '^[[:space:]]*-[[:space:]]\[[[:space:]]\]' <<<"$body")"

if [[ "$unchecked" -gt 0 ]]; then
    reason="이슈 #${issue_number}의 TODO 체크리스트에 아직 체크되지 않은 항목이 ${unchecked}개 있다. 완료된 항목은 'gh issue edit ${issue_number}'로 체크(- [x])하고, 아직 안 끝난 항목이 있다면 이번 PR 범위에 포함할지부터 판단해라."
    jq -n --arg reason "$reason" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
    exit 0
fi

exit 0
