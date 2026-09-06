#!/usr/bin/env bash
# PreToolUse(Bash) 훅: git commit / gh pr create,edit 명령에 Claude 어트리뷰션 트레일러가 섞이면 차단한다.
#
# 배경(이슈 #25): 세션의 어트리뷰션 기본값이 Co-Authored-By/Claude-Session 트레일러를 붙이라고 지시해도
# 이 저장소는 그 트레일러를 원하지 않는다(commit-push 스킬 참고). 사람 기억에 의존하지 않고
# 훅으로 결정론적으로 막는다.
# heredoc(-m/--body "$(cat <<'EOF' ... EOF)")으로 작성된 메시지도 command 문자열 안에 그대로
# 들어있으므로 실행 전 raw command 텍스트를 그대로 검사한다.
# -m 없이 에디터로 메시지를 작성하는 커밋(예: --amend --no-edit)은 최종 메시지를 알 수 없으므로
# 검증하지 않고 통과시킨다(fail-open).

payload="$(cat)"
command="$(jq -r '.tool_input.command // empty' <<<"$payload" 2>/dev/null)"

is_git_commit=false
is_gh_pr=false

[[ "$command" =~ (^|[[:space:]\&\|\;])git[[:space:]]+commit ]] && is_git_commit=true
[[ "$command" =~ (^|[[:space:]\&\|\;])gh[[:space:]]+pr[[:space:]]+(create|edit) ]] && is_gh_pr=true

if [[ "$is_git_commit" == false && "$is_gh_pr" == false ]]; then
    exit 0
fi

if [[ "$is_git_commit" == true && "$is_gh_pr" == false ]]; then
    [[ "$command" =~ (^|[[:space:]])-m([[:space:]]|=) ]] || exit 0
fi

if grep -qiE 'co-authored-by:[[:space:]]*claude|claude-session:' <<<"$command"; then
    reason="git commit/gh pr 명령에 Claude 어트리뷰션 트레일러(Co-Authored-By/Claude-Session)가 포함되어 있다. 이 저장소는 그 트레일러를 원하지 않으니(commit-push 스킬 참고) 트레일러를 제거하고 다시 실행해라."
    jq -n --arg reason "$reason" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
    exit 0
fi

exit 0
