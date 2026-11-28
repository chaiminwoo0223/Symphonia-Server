#!/usr/bin/env bash
# BJCP와 X-Wines 원본을 VM에 올리고 적재 컨테이너를 한 번 실행한다. deploy.sh로 배포한 뒤에 실행한다.
source "$(dirname "$0")/common.sh"

"${ROOT}/scripts/import/fetch-bjcp.sh"

upload "${ROOT}/scripts/import/data/bjcp" "${ROOT}/scripts/import/data/x-wines"
vm_ssh "sudo bash /opt/mariajou/remote.sh import"
