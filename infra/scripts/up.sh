#!/usr/bin/env bash
# persistent와 app을 만든다. Terraform이 계획을 보여 주고 확인을 받은 뒤에 적용한다.
source "$(dirname "$0")/common.sh"

"${INFRA}/scripts/bootstrap.sh"

tf_init persistent
# 처음 실행할 때는 API가 꺼져 있어 조회가 실패하므로, 만든 리소스가 있을 때만 중지 여부를 확인한다.
if has_resources persistent; then
    resume_if_stopped
fi

# DB 사용자를 만들기 전에 비밀번호가 등록돼 있어야 해서, 처음 한 번은 secret부터 만든다.
if ! secret_has_version DB_PASSWORD; then
    log "secret을 먼저 만듭니다"
    TF_VAR_db_password="unused" tf persistent apply -target=google_secret_manager_secret.app
fi
"${INFRA}/scripts/secrets.sh"

log "persistent를 적용합니다"
export_db_password
tf persistent apply

log "app을 적용합니다"
tf_init app
tf app apply

log "완료했습니다. DuckDNS의 IP를 $(tf persistent output -raw app_ip)로 바꾼 뒤 deploy.sh를 실행하세요"
