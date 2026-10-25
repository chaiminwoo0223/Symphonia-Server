#!/usr/bin/env bash
# PostToolUse(Edit|Write) 훅: .java 파일 수정 시 그 파일 하나만 Spotless(Google Java Format)로 자동 포맷.
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
if [[ -f "$STRIP_AWK" && "$file" == "$ROOT/src/main/"* && -f "$file" ]]; then
    awk -f "$STRIP_AWK" "$file" > "$file.tmp" && mv "$file.tmp" "$file"
fi

[[ -f "./gradlew" ]] || exit 0

# spotlessIdeHook은 Spotless가 IDE 연동용으로 제공하는 속성으로, 지정한 파일 하나만 포맷한다.
if ! output="$(./gradlew spotlessApply -PspotlessIdeHook="$file" --quiet 2>&1)"; then
    echo "⚠️ spotlessApply 실패:" >&2
    echo "$output" | tail -20 >&2
fi

exit 0
