#!/usr/bin/env bash
# PostToolUse(Edit|Write) 훅: .java 파일 수정 시 Spotless(Google Java Format) 자동 포맷.
# 실패해도 편집 자체를 막지 않는다(항상 exit 0, 실패를 열어둔다).

payload="$(cat)"
file="$(jq -r '.tool_input.file_path // .tool_response.filePath // empty' <<<"$payload" 2>/dev/null)"

if [[ "$file" != *.java ]]; then
    exit 0
fi

ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || echo .)}"
cd "$ROOT" || exit 0

[[ -f "./gradlew" ]] || exit 0

if ! output="$(./gradlew spotlessApply --quiet 2>&1)"; then
    echo "⚠️ spotlessApply 실패:" >&2
    echo "$output" | tail -20 >&2
fi

exit 0
