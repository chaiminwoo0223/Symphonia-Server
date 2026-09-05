#!/usr/bin/env bash
# Stop 훅: 직전 응답에서 필러 대시(-, —) 사용과 문장 중간 줄바꿈을 검사한다.
# 실패를 열어둔다. jq/awk 오류나 파싱 실패 시 아무것도 하지 않고 조용히 통과한다.
set -uo pipefail

payload="$(cat)"

stop_hook_active="$(jq -r '.stop_hook_active // false' <<<"$payload" 2>/dev/null)"
if [[ "$stop_hook_active" == "true" ]]; then
    exit 0
fi

transcript_path="$(jq -r '.transcript_path // empty' <<<"$payload" 2>/dev/null)"
if [[ -z "$transcript_path" || ! -r "$transcript_path" ]]; then
    exit 0
fi

text=""
while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    if jq -e 'select((.type == "assistant") or (.message.role == "assistant"))' <<<"$line" >/dev/null 2>&1; then
        candidate="$(jq -r '[(.message.content // [])[]? | select(.type == "text") | .text] | join("\n")' <<<"$line" 2>/dev/null)"
        if [[ -n "$candidate" ]]; then
            text="$candidate"
            break
        fi
    fi
done < <(awk '{ a[NR] = $0 } END { for (i = NR; i >= 1; i--) print a[i] }' "$transcript_path" 2>/dev/null)

if [[ -z "$text" ]]; then
    exit 0
fi

violations="$(awk '
function is_structural(s) {
    return (s ~ /^[-*] / || s ~ /^[0-9]+\. / || s ~ /^#/ || s ~ /^\|/ || s ~ /^>/)
}
function ends_sentence(s,    c) {
    c = substr(s, length(s), 1)
    return (index(".!?:)\"'"'"')]}", c) > 0)
}
BEGIN { in_code = 0; prev = ""; have_prev = 0 }
{
    line = $0
    if (line ~ /^```/) { in_code = !in_code; next }
    if (in_code) next

    clean = line
    gsub(/`[^`]*`/, "", clean)

    if (index(clean, "—") > 0) {
        print "em dash(—)를 문장 연결에 사용했다. 두 문장으로 나눠 완전한 문장으로 쓸 것."
    }

    stripped = clean
    sub(/^[ \t]+/, "", stripped)
    sub(/[ \t]+$/, "", stripped)

    if (stripped == "") { have_prev = 0; next }

    if (!is_structural(stripped) && index(stripped, " - ") > 0) {
        print "하이픈(-)을 문장 연결어로 사용한 것으로 보인다: " stripped
    }

    if (have_prev && !is_structural(prev) && !is_structural(stripped) && !ends_sentence(prev)) {
        print "문장을 끝내지 않고 줄바꿈된 것으로 보인다: ..." prev
    }

    prev = stripped
    have_prev = 1
}
' <<<"$text")"

if [[ -z "$violations" ]]; then
    exit 0
fi

reason="직전 응답이 글쓰기 스타일 규칙을 위반했다. 대시(-, —)를 문장 연결어로 쓰지 말고, 문장 중간에 줄바꿈하지 말고, 완전한 문장으로 다시 써라.
$(sed 's/^/- /' <<<"$violations")"

jq -n --arg reason "$reason" '{decision: "block", reason: $reason}'
exit 0
