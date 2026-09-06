#!/usr/bin/env bash
# PreToolUse(Bash) 훅: 커밋 메시지 안의 모든 type이 서로 같은지 검증한다.
#
# commit-push 스킬 규칙: "같은 type끼리는 하나의 커밋으로 묶고, type이 다르면 커밋 자체를 나눈다."
# 상단 "[#이슈번호] type: 설명"의 type과, 하위 "* type: 설명" 불릿들의 type을 전부 뽑아
# 서로 다른 값이 섞여 있으면 커밋을 막는다.
# heredoc(-m "$(cat <<'EOF' ... EOF)")으로 작성된 메시지도 command 문자열 안에 그대로 들어있으므로
# 실행 전 raw command 텍스트를 그대로 검사한다.
# -m 없이 에디터로 메시지를 작성하는 커밋(예: --amend --no-edit)은 최종 메시지를 알 수 없으므로 검증하지 않고 통과시킨다(fail-open).

payload="$(cat)"
command="$(jq -r '.tool_input.command // empty' <<<"$payload" 2>/dev/null)"

if [[ ! "$command" =~ (^|[[:space:]\&\|\;])git[[:space:]]+commit ]]; then
    exit 0
fi

[[ "$command" =~ (^|[[:space:]])-m([[:space:]]|=) ]] || exit 0

top_type="$(grep -oE '\[#[0-9]+\][[:space:]]*[a-z]+:' <<<"$command" \
    | head -1 \
    | sed -E 's/^\[#[0-9]+\][[:space:]]*([a-z]+):.*/\1/')"

bullet_types="$(grep -oE '^\*[[:space:]]*[a-z]+:' <<<"$command" \
    | sed -E 's/^\*[[:space:]]*([a-z]+):.*/\1/')"

all_types="$(printf '%s\n%s\n' "$top_type" "$bullet_types" | grep -v '^$' | sort -u)"
type_count="$(grep -c . <<<"$all_types" 2>/dev/null || echo 0)"

if [[ "$type_count" -gt 1 ]]; then
    types_joined="$(tr '\n' ',' <<<"$all_types" | sed 's/,$//')"
    reason="커밋 메시지 안에 서로 다른 type이 섞여 있다 (${types_joined}). commit-push 스킬 규칙상 하나의 커밋에는 하나의 type만 담아야 하니, type별로 커밋을 나눠서 다시 커밋해라."
    jq -n --arg reason "$reason" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
    exit 0
fi

exit 0
