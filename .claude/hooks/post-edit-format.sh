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

# 타입 선언(class/interface/enum/record)의 여는 중괄호 바로 다음 줄에 빈 줄을 두지 않는다.
# Google Java Format(Spotless)은 사람이 넣은 단일 빈 줄을 지우지 않아 이 부분은 걸러주지 못하므로,
# 여기서 결정론적으로 제거한다(판단이 아니라 기계적 규칙이라 스크립트로 강제, code-style 스킬 참고).
# src/test는 제외한다(testing 스킬의 이중 @Nested 구조는 이 빈 줄을 의도적으로 사용).
STRIP_AWK="$ROOT/.claude/scripts/strip-type-open-blank-line.awk"
if [[ -f "$STRIP_AWK" ]]; then
    while IFS= read -r -d '' java_file; do
        awk -f "$STRIP_AWK" "$java_file" > "$java_file.tmp" && mv "$java_file.tmp" "$java_file"
    done < <(find "$ROOT/src/main" -name "*.java" -print0 2>/dev/null)
fi

[[ -f "./gradlew" ]] || exit 0

if ! output="$(./gradlew spotlessApply --quiet 2>&1)"; then
    echo "⚠️ spotlessApply 실패:" >&2
    echo "$output" | tail -20 >&2
fi

exit 0
