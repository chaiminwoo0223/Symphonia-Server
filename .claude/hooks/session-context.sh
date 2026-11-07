#!/usr/bin/env bash
# SessionStart 훅: 세션 시작 시 현재 브랜치와 gh 활성 계정을 컨텍스트로 알려 준다.
#
# gh에 저장소 소유자 계정 외의 다른 계정도 함께 로그인되어 있어, 잘못된 계정으로 이슈나 PR을 만드는 일을 막기 위해 둔다(이슈 #133).
# 기대 계정은 git 작성자 이메일에서 정한다(이슈 #170). 같은 머신에서 다른 사람이 다른 git 작성자로 작업하기 때문이다.
# 활성 계정은 네트워크 호출 없이 gh 로컬 설정에서 읽는다.
# 정보를 못 읽어도 세션 시작을 막지 않는다(항상 exit 0, 실패를 열어둔다).

source "$(dirname "$0")/lib.sh"

cd_project_root || exit 0

if branch="$(git symbolic-ref -q --short HEAD 2>/dev/null)"; then
    :
elif git rev-parse --verify HEAD >/dev/null 2>&1; then
    branch="detached HEAD"
fi
gh_user="$(active_gh_user)"
expected="$(expected_gh_user)"

echo "현재 브랜치: ${branch:-알 수 없음}"
echo "gh 활성 계정: ${gh_user:-알 수 없음}"
echo "기대 계정: ${expected:-알 수 없음(git 작성자 이메일이 noreply 형식이 아니라 계정 게이트가 동작하지 않는다)}"

if [[ -n "$gh_user" && -n "$expected" && "$gh_user" != "$expected" ]]; then
    echo "경고: gh 활성 계정이 git 작성자 계정 ${expected}와 다르다. gh로 이슈나 PR을 만들기 전에 'gh auth switch -u ${expected}'로 계정을 바꿔라."
fi

exit 0
