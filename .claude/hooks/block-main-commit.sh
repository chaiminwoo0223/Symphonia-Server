#!/usr/bin/env bash
# PreToolUse(Bash) 훅: `main` 브랜치에서의 직접 커밋을 차단한다.
#
# main은 운영 배포 브랜치라 로컬에서 직접 커밋하는 변경이 있으면 안 된다(2026-09-05).
# develop은 [HotFix] 커밋으로 직접 해결하는 경우가 있으므로 이 훅의 대상이 아니다.
# 브랜치 판별에 실패하는 상황(git 저장소 밖 등)에서는 항상 통과시킨다(fail-open).

source "$(dirname "$0")/lib.sh"

read_command
is_git_commit || exit 0
cd_project_root || exit 0

[[ "$(current_branch)" == "main" ]] || exit 0

deny "main 브랜치는 직접 커밋할 수 없다. develop에서 분기한 <type>/<이슈번호> 브랜치(예: feat/9)에서 작업한 뒤 PR로 병합해라."
