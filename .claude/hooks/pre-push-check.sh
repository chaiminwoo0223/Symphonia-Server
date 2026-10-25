#!/usr/bin/env bash
# PreToolUse(Bash) 훅: `git push` 직전 테스트 컨벤션 검사와 전체 빌드+테스트를 강제한다.
#
# check-all.sh(check-test-conventions.sh + ./gradlew clean build)로 게이트를 건다.
# push마다 수 분이 걸릴 수 있음을 감수하기로 했다(2026-09-04).
# 실패해도 push를 막지 못하는 상황(스크립트 오류 등)에서는 항상 통과시킨다(fail-open).

source "$(dirname "$0")/lib.sh"

read_command
[[ "$command" =~ (^|[[:space:]\&\|\;])git[[:space:]]+push ]] || exit 0
cd_project_root || exit 0

[[ -x "./.claude/scripts/check-all.sh" ]] || exit 0

if output="$(./.claude/scripts/check-all.sh 2>&1)"; then
    exit 0
fi

deny "git push 전 전체 검증(check-all.sh)이 실패했다. 문제를 해결한 뒤 다시 push해라.
$(tail -30 <<<"$output")"
