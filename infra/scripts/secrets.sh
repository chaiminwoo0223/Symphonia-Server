#!/usr/bin/env bash
# 값이 없는 secret에만 첫 버전을 등록한다. 이미 있는 값은 바꾸지 않는다.
# 내부 비밀값은 만들어 넣고, 외부 서비스에서 발급받은 값은 입력받는다. 값은 화면과 명령 인자에 남기지 않는다.
source "$(dirname "$0")/common.sh"

add_version() { printf '%s' "$2" | gc secrets versions add "$1" --data-file=- > /dev/null; }

while read -r secret <&3; do
    [ -n "$secret" ] || continue
    secret_exists "$secret" || die "secret이 없습니다: ${secret}. up.sh를 먼저 실행하세요"
    secret_has_version "$secret" && continue

    case "$secret" in
        DB_PASSWORD | REDIS_PASSWORD) value="$(openssl rand -hex 24)" ;;
        JWT_SECRET) value="$(openssl rand -base64 48 | tr -d '\n')" ;;
        GATHERING_MASTER_KEY) value="$(openssl rand -base64 32)" ;;
        *)
            read -r -s -p "${secret} 값을 입력하세요: " value
            echo
            ;;
    esac

    [ -n "$value" ] || die "빈 값은 등록할 수 없습니다: ${secret}"
    case "$value" in *"'"*) die "작은따옴표가 든 값은 쓸 수 없습니다: ${secret}" ;; esac
    add_version "$secret" "$value"
    log "등록했습니다: ${secret}"

    if [ "$secret" = GATHERING_MASTER_KEY ]; then
        echo "  이 값을 잃으면 참여자 정보를 복호화할 수 없습니다. 아래 명령으로 확인해 비밀번호 관리자에 보관하세요."
        echo "  gcloud --project ${PROJECT_ID} secrets versions access latest --secret=GATHERING_MASTER_KEY"
    fi
done 3< "${INFRA}/secrets.txt"
