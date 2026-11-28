#!/usr/bin/env bash
# dev DB를 압축한 SQL로 로컬에 받는다. 암호화한 참여자 정보가 들어 있으므로 저장소 밖에 두고, 필요 없으면 지운다.
source "$(dirname "$0")/common.sh"

vm_exists || die "앱 VM이 없어 덤프할 수 없습니다. up.sh와 deploy.sh를 먼저 실행하세요"
resume_if_stopped
wait_for_vm

tf_init persistent
target="${DUMP_DIR:-$HOME}/${NAME}-$(date +%Y%m%d-%H%M%S).sql.gz"

umask 077
trap 'rm -f "${target}.tmp"' EXIT
vm_ssh "sudo bash /opt/mariajou/remote.sh dump \
    '$(tf persistent output -raw db_host)' '$(tf persistent output -raw db_name)' '$(tf persistent output -raw db_username)'" > "${target}.tmp"
gzip -t "${target}.tmp"
mv "${target}.tmp" "$target"
log "덤프를 저장했습니다: ${target}"
