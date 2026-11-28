#!/usr/bin/env bash
# infra 스크립트가 함께 쓰는 설정과 함수다. 직접 실행하지 않고 source한다. macOS의 bash 3.2에서 동작해야 한다.
set -euo pipefail

# 다른 프로젝트를 실수로 지우지 않도록, gcloud의 현재 설정을 따르지 않고 항상 명시하게 한다.
: "${PROJECT_ID:?PROJECT_ID 환경변수에 dev 전용 GCP 프로젝트 ID를 지정하세요}"
REGION="${REGION:-asia-northeast3}"
ZONE="${ZONE:-asia-northeast3-a}"
NAME="mariajou-dev"
STATE_BUCKET="${PROJECT_ID}-tfstate"
APP_VM="${NAME}-app"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
INFRA="${ROOT}/infra"

export TF_VAR_project_id="$PROJECT_ID" TF_VAR_region="$REGION" TF_VAR_zone="$ZONE"

log() { printf '==> %s\n' "$*"; }
die() { printf '✗ %s\n' "$*" >&2; exit 1; }

gc() { gcloud --project "$PROJECT_ID" "$@"; }

tf() {
    local module="$1"
    shift
    terraform -chdir="${INFRA}/${module}" "$@"
}

tf_init() {
    tf "$1" init -input=false -reconfigure -backend-config="bucket=${STATE_BUCKET}" > /dev/null
}

bucket_exists() { gc storage buckets describe "gs://${STATE_BUCKET}" > /dev/null 2>&1; }

# 초기화한 모듈의 상태에 리소스가 남아 있는지 확인한다.
# 조회 실패를 "없음"으로 보면 teardown.sh가 리소스를 남긴 채 상태를 지울 수 있어, 상태가 없는 경우가 아니면 중단한다.
has_resources() {
    local list
    if ! list="$(tf "$1" state list 2>&1)"; then
        case "$list" in
            *"No state file was found"*) return 1 ;;
        esac
        die "$1의 상태를 읽지 못했습니다: ${list}"
    fi
    [ -n "$list" ]
}

db_instance() { gc sql instances list --filter="name~^${NAME}-db-" --format='value(name)'; }

vm_exists() { [ -n "$(gc compute instances list --filter="name=${APP_VM}" --format='value(name)')" ]; }

secret_exists() { gc secrets describe "$1" > /dev/null 2>&1; }

secret_has_version() {
    [ -n "$(gc secrets versions list "$1" --filter='state=ENABLED' --format='value(name)' 2> /dev/null)" ]
}

secret_value() { gc secrets versions access latest --secret="$1"; }

# destroy와 보호 해제에는 비밀번호가 쓰이지 않지만, 변수 값은 있어야 한다.
export_db_password() {
    if secret_has_version DB_PASSWORD; then
        TF_VAR_db_password="$(secret_value DB_PASSWORD)"
    else
        TF_VAR_db_password="unused"
    fi
    export TF_VAR_db_password
}

# OS Login에 등록한 SSH 공개 키가 계정에 계속 남지 않게 만료 시간을 둔다.
SSH_KEY_TTL=1h

vm_ssh() {
    gc compute ssh "$APP_VM" --zone "$ZONE" --tunnel-through-iap --ssh-key-expire-after="$SSH_KEY_TTL" --command "$1"
}

# 시작 스크립트가 Docker 설치를 마칠 때까지 기다린다.
wait_for_vm() {
    local i=0
    log "VM의 시작 스크립트가 끝나기를 기다립니다"
    until vm_ssh 'sudo test -d /opt/mariajou && sudo docker compose version' > /dev/null 2>&1; do
        i=$((i + 1))
        [ "$i" -lt 40 ] || die "VM이 준비되지 않았습니다. 'sudo journalctl -u google-startup-scripts'를 확인하세요"
        /bin/sleep 10
    done
}

# 로컬 파일을 VM의 /opt/mariajou 아래에 올린다. 인자는 로컬 경로 목록이다.
upload() {
    vm_ssh 'rm -rf /tmp/mariajou-upload && mkdir -m 700 /tmp/mariajou-upload'
    gc compute scp --zone "$ZONE" --tunnel-through-iap --ssh-key-expire-after="$SSH_KEY_TTL" --recurse \
        "$@" "${APP_VM}:/tmp/mariajou-upload/"
}

resume_if_stopped() {
    local db
    db="$(db_instance)"
    if [ -n "$db" ] && [ "$(gc sql instances describe "$db" --format='value(settings.activationPolicy)')" = "NEVER" ]; then
        log "중지된 DB를 다시 시작합니다"
        gc sql instances patch "$db" --activation-policy=ALWAYS --quiet
    fi
    if vm_exists && [ "$(gc compute instances describe "$APP_VM" --zone "$ZONE" --format='value(status)')" = "TERMINATED" ]; then
        log "중지된 VM을 다시 시작합니다"
        gc compute instances start "$APP_VM" --zone "$ZONE"
    fi
}
