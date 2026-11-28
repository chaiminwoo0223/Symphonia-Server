#!/usr/bin/env bash
# dev 전용 프로젝트에 비용이나 흔적을 남기는 리소스가 있는지 확인한다. 하나라도 남거나 조회하지 못하면 실패한다.
source "$(dirname "$0")/common.sh"

failed=0
errors="$(mktemp)"
trap 'rm -f "$errors"' EXIT

# 첫 인자는 항목 이름이고, 나머지는 남은 리소스를 한 줄에 하나씩 출력하는 gcloud 명령이다.
# gcloud는 안내 문구를 표준 오류로 쓰므로 표준 출력만 남은 리소스로 본다.
check() {
    local label="$1" output
    shift
    if ! output="$(gc "$@" --quiet 2> "$errors")"; then
        if ! grep -qE 'has not been used|is not enabled|SERVICE_DISABLED' "$errors"; then
            printf '✗ %s: 조회하지 못했습니다\n' "$label"
            cat "$errors"
            failed=1
        fi
    elif [ -n "$output" ]; then
        printf '✗ %s 항목이 남았습니다\n%s\n' "$label" "$output"
        failed=1
    fi
}

check "VM" compute instances list --format='value(name)'
check "디스크" compute disks list --format='value(name)'
check "스냅샷" compute snapshots list --format='value(name)'
check "고정 IP" compute addresses list --format='value(name)'
check "방화벽 규칙" compute firewall-rules list --filter="name~^${NAME}" --format='value(name)'
check "VPC" compute networks list --filter="name~^${NAME}" --format='value(name)'
check "Cloud SQL" sql instances list --format='value(name)'
check "Artifact Registry" artifacts repositories list --format='value(name)'
check "secret" secrets list --format='value(name)'
check "서비스 계정" iam service-accounts list --filter="email~^${NAME}" --format='value(email)'
check "버킷" storage buckets list --format='value(name)'

[ "$failed" -eq 0 ] || die "남은 항목이 있습니다"
log "남은 리소스가 없습니다"
