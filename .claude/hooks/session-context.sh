#!/usr/bin/env bash
# SessionStart 훅: 세션 시작 시 현재 브랜치와 gh 활성 계정을 컨텍스트로 알려 준다.
#
# gh에 저장소 소유자 계정 외의 다른 계정도 함께 로그인되어 있어, 잘못된 계정으로 이슈나 PR을 만드는 일을 막기 위해 둔다(이슈 #133).
# 활성 계정은 네트워크 호출 없이 gh 로컬 설정에서 읽는다.
# 정보를 못 읽어도 세션 시작을 막지 않는다(항상 exit 0, 실패를 열어둔다).

EXPECTED_GH_USER="chaiminwoo0223"

ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || echo .)}"
cd "$ROOT" || exit 0

if branch="$(git symbolic-ref -q --short HEAD 2>/dev/null)"; then
    :
elif git rev-parse --verify HEAD >/dev/null 2>&1; then
    branch="detached HEAD"
fi
gh_user="$(gh config get -h github.com user 2>/dev/null)"

echo "현재 브랜치: ${branch:-알 수 없음}"
echo "gh 활성 계정: ${gh_user:-알 수 없음}"

if [[ -n "$gh_user" && "$gh_user" != "$EXPECTED_GH_USER" ]]; then
    echo "경고: gh 활성 계정이 ${EXPECTED_GH_USER}가 아니다. gh로 이슈나 PR을 만들기 전에 'gh auth switch -u ${EXPECTED_GH_USER}'로 계정을 바꿔라."
fi

exit 0
