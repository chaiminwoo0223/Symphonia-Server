#!/usr/bin/env bash
# PR 전 전체 검증 스크립트
# 실행: ./.claude/scripts/check-all.sh

set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

echo "========================================"
echo "  PR 전 전체 검증"
echo "========================================"

echo ""
echo "[1/1] 빌드 + 전체 테스트..."
./gradlew clean build
echo "✓ 빌드 및 테스트 통과"

echo ""
echo "========================================"
echo "  모든 검증 통과! PR 준비 완료."
echo "========================================"
