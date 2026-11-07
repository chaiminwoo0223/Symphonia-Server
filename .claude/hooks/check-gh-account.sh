#!/usr/bin/env bash
# PreToolUse(Bash) 훅: gh 활성 계정이 git 작성자 계정과 다르면 gh issue/gh pr 쓰기 명령을 차단한다.
#
# 배경(이슈 #170): SessionStart 경고만으로는 다른 계정으로 이슈나 PR을 만드는 일을 막지 못한다.
# 기대 계정은 git 작성자 이메일(noreply 형식)에서 정한다. 읽기와 로컬 명령(view, list, status, checks, diff, checkout)은 계정과 무관하므로 통과시킨다.
# 활성 계정은 gh 로컬 설정에서 읽는다. GH_TOKEN, GITHUB_TOKEN 환경변수로 계정을 덮어쓴 경우는 판별하지 않는다(네트워크 호출 없이 판정하기 위해서다).
# `gh -R owner/repo pr create`처럼 gh 바로 뒤에 전역 플래그를 둔 형태와 gh api 쓰기는 판별하지 않는다.
# 기대 계정이나 활성 계정을 못 읽으면 막지 않는다(fail-open).

source "$(dirname "$0")/lib.sh"

# 복합 명령(`gh issue view 1 && gh pr create`)의 각 gh issue/pr 명령을 차례로 보고, 쓰기 명령이 하나라도 있으면 0, 없으면 1을 돌려준다.
has_gh_write() {
    local rest="$command"
    while [[ "$rest" =~ (^|[[:space:]\&\|\;\(])gh[[:space:]]+(issue|pr)[[:space:]]+([a-z-]+)(.*)$ ]]; do
        rest="${BASH_REMATCH[4]}"
        case "${BASH_REMATCH[3]}" in
            view | list | status | checks | diff | checkout) ;;
            *) return 0 ;;
        esac
    done
    return 1
}

read_command
has_gh_write || exit 0
cd_project_root || exit 0

expected="$(expected_gh_user)" || exit 0
active="$(active_gh_user)"
[[ -z "$active" || "$active" == "$expected" ]] && exit 0

deny "gh 활성 계정(${active})이 git 작성자 계정(${expected})과 다르다. 'gh auth switch -u ${expected}'로 바꾼 뒤 다시 실행해라."
