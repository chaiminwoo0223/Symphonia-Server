#!/usr/bin/env bash
# BJCP 2021 스타일 가이드라인 JSON(beerjson/bjcp-json) 다운로드 스크립트
# 실행: ./scripts/import/fetch-bjcp.sh
#
# 가이드라인 텍스트의 저작권은 BJCP에 있으므로 원본을 커밋하지 않는다.
# 대신 고정 커밋에서 받은 파일을 체크섬으로 검증한다.

set -euo pipefail

COMMIT="fe9063dff1e86c3aa9d8c65a1c730b4a807e48c3"
URL="https://raw.githubusercontent.com/beerjson/bjcp-json/${COMMIT}/scripts/bjcp_styleguide-2021.json"
SHA256="df2eb7572f4dc5279e812dc94c7305e3399b5b8ab7a16b1d93d3d777ab57fbf4"

DATA_DIR="$(cd "$(dirname "$0")" && pwd)/data/bjcp"
TARGET="${DATA_DIR}/bjcp_styleguide-2021.json"

mkdir -p "$DATA_DIR"
curl -fsSL "$URL" -o "${TARGET}.tmp"

actual="$(shasum -a 256 "${TARGET}.tmp" | awk '{print $1}')"
if [ "$actual" != "$SHA256" ]; then
    rm -f "${TARGET}.tmp"
    echo "✗ 체크섬 불일치: expected=${SHA256} actual=${actual}" >&2
    exit 1
fi

mv "${TARGET}.tmp" "$TARGET"
echo "✓ BJCP JSON 다운로드 완료: ${TARGET} ($(jq '.beerjson.styles | length' "$TARGET")개 스타일)"
