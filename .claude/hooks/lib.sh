#!/usr/bin/env bash
# PreToolUse(Bash) 훅 공통 함수. 각 훅이 `source "$(dirname "$0")/lib.sh"`로 불러 쓴다.
#
# 입력 파싱, 명령 판별, 브랜치 조회, 차단 응답 출력처럼 훅마다 똑같이 반복되던 코드를 한곳에 모은다.
# 판정 규칙 자체는 각 훅이 갖는다.

# stdin의 훅 payload에서 Bash 명령 문자열을 읽어 전역 변수 command에 담는다.
read_command() {
    local payload
    payload="$(cat)"
    command="$(jq -r '.tool_input.command // empty' <<<"$payload" 2>/dev/null)"
}

is_git_commit() {
    [[ "$command" =~ (^|[[:space:]\&\|\;])git[[:space:]]+commit ]]
}

# -m 없이 에디터로 메시지를 쓰는 커밋(예: --amend --no-edit)은 최종 메시지를 알 수 없어 검증하지 않는다(fail-open).
has_message_flag() {
    [[ "$command" =~ (^|[[:space:]])-m([[:space:]]|=) ]]
}

# 프로젝트 루트로 이동한다. 실패하면 호출한 훅이 통과시키도록 0이 아닌 값을 돌려준다.
cd_project_root() {
    cd "${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || echo .)}"
}

current_branch() {
    git rev-parse --abbrev-ref HEAD 2>/dev/null
}

# <type>/<이슈번호> 브랜치면 이슈 번호를 출력하고, 아니면 1을 돌려준다.
branch_issue_number() {
    local branch
    branch="$(current_branch)"
    [[ "$branch" =~ ^[a-z]+/([0-9]+)$ ]] || return 1
    echo "${BASH_REMATCH[1]}"
}

# PreToolUse 차단 응답을 출력하고 훅을 끝낸다.
deny() {
    jq -n --arg reason "$1" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
    exit 0
}
