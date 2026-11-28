#!/usr/bin/env bash
# dev 환경을 모두 지운다. DB 데이터와 비밀값을 되돌릴 수 없다.
source "$(dirname "$0")/common.sh"

bucket_exists || die "상태 버킷이 없습니다. check-leftovers.sh로 남은 리소스를 확인하세요"

echo "프로젝트 ${PROJECT_ID}의 dev 환경을 모두 지웁니다. DB 데이터와 비밀값은 복구할 수 없습니다."
read -r -p "계속하려면 프로젝트 ID를 입력하세요: " answer
[ "$answer" = "$PROJECT_ID" ] || die "입력이 달라 중단합니다"

read -r -p "지우기 전에 DB를 덤프할까요? (y/n): " answer
case "$answer" in
    y | Y)
        "${INFRA}/scripts/dump-db.sh"
        echo "덤프를 복원해 참여자 정보를 읽으려면 GATHERING_MASTER_KEY도 보관해야 합니다."
        read -r -p "보관을 마쳤으면 Enter를 누르세요: " _
        ;;
    n | N) ;;
    *) die "y 또는 n으로 답하세요" ;;
esac

"${INFRA}/scripts/down.sh"

tf_init persistent
if has_resources persistent; then
    resume_if_stopped
    export_db_password
    export TF_VAR_db_deletion_protection=false
    # 삭제 보호는 상태에 먼저 반영해야 destroy가 인스턴스를 지울 수 있다.
    if [ -n "$(db_instance)" ]; then
        log "DB 삭제 보호를 해제합니다"
        tf persistent apply -target=google_sql_database_instance.main
    fi
    log "persistent를 지웁니다"
    tf persistent destroy
fi

for module in persistent app loadgen; do
    tf_init "$module"
    if has_resources "$module"; then
        die "${module}에 리소스가 남아 상태 버킷을 지우지 않습니다"
    fi
done

log "상태 버킷을 지웁니다"
gc storage rm --recursive "gs://${STATE_BUCKET}"
rm -rf "${INFRA}"/*/.terraform

"${INFRA}/scripts/check-leftovers.sh"

cat <<EOF

남은 수동 작업입니다.
1. DuckDNS에서 mariajou.duckdns.org 레코드를 지우거나 바꾸세요. 반납한 IP를 다른 사람이 받으면 도메인이 그 서버로 연결됩니다.
2. 프로젝트까지 없애려면 'gcloud projects delete ${PROJECT_ID}'를 실행하세요. 30일의 복구 대기 뒤에 완전히 삭제됩니다.
3. 다른 GCP 작업에 쓰지 않는다면 로컬의 ~/.ssh/google_compute_engine 키를 지우세요.
EOF
