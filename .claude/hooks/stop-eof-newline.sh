#!/usr/bin/env bash
# Stop 훅: 세션 종료 시 git이 추적하는 텍스트 파일의 EOF 개행을 자동 보정한다.
# 실패해도 세션 종료를 막지 않는다(항상 exit 0, 실패를 열어둔다).

ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || echo .)}"
cd "$ROOT" || exit 0

BINARY_EXTENSIONS="jar png jpg jpeg gif ico svg woff woff2 ttf class keystore p12"

while IFS= read -r file; do
    [[ -f "$file" ]] || continue
    [[ -s "$file" ]] || continue

    ext_lower=$(echo "${file##*.}" | tr '[:upper:]' '[:lower:]')
    for bin_ext in $BINARY_EXTENSIONS; do
        [[ "$ext_lower" == "$bin_ext" ]] && continue 2
    done

    last_byte=$(tail -c 1 "$file" | od -An -tx1 | tr -d ' ')
    if [[ "$last_byte" != "0a" ]]; then
        printf '\n' >>"$file"
        echo "개행 문자 추가: $file"
    fi
done < <(git ls-files)

exit 0
