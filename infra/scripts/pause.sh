#!/usr/bin/env bash
# VM과 DB를 중지한다. 데이터, 디스크, 고정 IP는 남고 보관 비용이 계속 발생한다.
source "$(dirname "$0")/common.sh"

if vm_exists; then
    log "VM을 중지합니다"
    gc compute instances stop "$APP_VM" --zone "$ZONE"
fi

db="$(db_instance)"
if [ -n "$db" ]; then
    log "DB를 중지합니다"
    gc sql instances patch "$db" --activation-policy=NEVER --quiet
fi

[ -z "$(gc compute instances list --filter="name=${NAME}-k6" --format='value(name)')" ] ||
    echo "k6 VM이 남아 있습니다. loadgen.sh down으로 지우세요."
