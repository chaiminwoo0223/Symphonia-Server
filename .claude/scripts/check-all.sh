#!/usr/bin/env bash
# PR 전 전체 검증 스크립트
# 실행: ./.claude/scripts/check-all.sh
#
# 로컬에서는 성공하면 한 줄만 출력하고, 실패하면 실패한 단계, 실패한 테스트, 로그 끝부분만 출력한다.
# 전체 로그는 .git/check-all.log에 남긴다. build는 clean이 지우므로 쓰지 않는다.
# CI 환경변수가 있으면 사람이 Actions 화면에서 원인을 찾을 수 있게 전체 로그를 그대로 출력한다.
# 작업 트리가 깨끗한 상태로 통과하면 HEAD를 기록한다. push 게이트는 기록과 HEAD가 같으면 검증을 건너뛴다.

set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

GIT_DIR="$(git rev-parse --absolute-git-dir)"
LOG="$GIT_DIR/check-all.log"
STEP_LOG="$GIT_DIR/check-all-step.log"
STAMP="$GIT_DIR/check-all-passed"
TAIL_LINES=20
# 스프링 애플리케이션 로그는 타임스탬프로 시작한다. 실패 원인이 아니라 종료 로그로 끝부분이 채워지므로 걸러 낸다.
APP_LOG_PATTERN='^[0-9]{4}-[0-9]{2}-[0-9]{2}T'

start_head="$(git rev-parse HEAD)"
start_status="$(git status --porcelain)"
rm -f "$STAMP"
: >"$LOG"

run_step() {
    local name="$1"
    shift

    if [[ -n "${CI:-}" ]]; then
        echo "[$name]"
        "$@"
        return
    fi

    if "$@" >"$STEP_LOG" 2>&1; then
        cat "$STEP_LOG" >>"$LOG"
        return
    fi

    cat "$STEP_LOG" >>"$LOG"
    echo "✗ $name 실패 (전체 로그: $LOG)"
    local failed_tests
    failed_tests="$(grep -E -A4 ' FAILED$' "$STEP_LOG" | grep -vE "$APP_LOG_PATTERN" | head -60 || true)"
    if [[ -n "$failed_tests" ]]; then
        echo ""
        echo "실패한 테스트:"
        echo "$failed_tests"
    fi
    echo ""
    echo "로그 끝부분:"
    grep -vE "$APP_LOG_PATTERN" "$STEP_LOG" | tail -n "$TAIL_LINES" || true
    exit 1
}

run_step "테스트 컨벤션 검사" ./.claude/scripts/check-test-conventions.sh
run_step "계층 규칙 검사" ./.claude/scripts/check-architecture.sh
run_step "빌드와 전체 테스트" ./gradlew clean build

if [[ -z "$start_status" && -z "$(git status --porcelain)" && "$(git rev-parse HEAD)" == "$start_head" ]]; then
    echo "$start_head" >"$STAMP"
fi

echo "✓ 전체 검증 통과 (테스트 컨벤션, 계층 규칙, 빌드와 전체 테스트)"
