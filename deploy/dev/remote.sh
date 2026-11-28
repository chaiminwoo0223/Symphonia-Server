#!/usr/bin/env bash
# VM에서 root로 실행한다. infra/scripts가 올린 뒤 호출하므로 직접 실행하지 않는다.
set -euo pipefail

APP_DIR=/opt/mariajou
UPLOAD_DIR=/tmp/mariajou-upload
ENV_FILE="${APP_DIR}/dev.env"

die() { printf '✗ %s\n' "$*" >&2; exit 1; }
compose() { docker compose --env-file "$ENV_FILE" -f "${APP_DIR}/compose.yaml" "$@"; }

metadata_token() {
    curl -fsS -H 'Metadata-Flavor: Google' \
        http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token |
        jq -r .access_token
}

# 비밀값은 VM의 서비스 계정으로 직접 읽는다. 로컬 머신과 명령 인자를 거치지 않는다.
secret() {
    printf 'header = "Authorization: Bearer %s"\n' "$2" |
        curl -fsS --config - \
            "https://secretmanager.googleapis.com/v1/projects/$1/secrets/$3/versions/latest:access" |
        jq -r .payload.data | base64 -d
}

write_env() {
    local project="$1" image="$2" db_url="$3" db_username="$4" acme_ca="$5" token name value
    token="$(metadata_token)"
    (
        umask 077
        {
            printf "APP_IMAGE='%s'\n" "$image"
            printf "DB_URL='%s'\n" "$db_url"
            printf "DB_USERNAME='%s'\n" "$db_username"
            [ -z "$acme_ca" ] || printf "ACME_CA='%s'\n" "$acme_ca"
            while read -r name; do
                [ -n "$name" ] || continue
                value="$(secret "$project" "$token" "$name")"
                printf "%s='%s'\n" "$name" "$value"
            done < "${APP_DIR}/secrets.txt"
        } > "${ENV_FILE}.tmp"
    )
    mv "${ENV_FILE}.tmp" "$ENV_FILE"
}

deploy() {
    local project="$1" image="$2" registry="${2%%/*}" i=0

    install -m 644 "${UPLOAD_DIR}/compose.yaml" "${UPLOAD_DIR}/Caddyfile" "${UPLOAD_DIR}/secrets.txt" "$APP_DIR/"
    install -m 700 "${UPLOAD_DIR}/remote.sh" "$APP_DIR/"
    # 앱 컨테이너는 root가 아닌 사용자라서 인증서를 읽을 수 있어야 한다.
    install -d -m 755 "${APP_DIR}/certs"
    install -m 644 "${UPLOAD_DIR}/server-ca.pem" "${APP_DIR}/certs/"
    rm -rf "$UPLOAD_DIR"

    write_env "$@"

    trap 'docker logout "'"$registry"'" > /dev/null 2>&1 || true' EXIT
    metadata_token | docker login -u oauth2accesstoken --password-stdin "$registry"
    compose pull
    compose up -d --remove-orphans
    # 파일 하나를 마운트하면 새로 설치한 Caddyfile이 실행 중인 컨테이너에 보이지 않아 다시 만든다.
    compose up -d --force-recreate caddy

    until curl -fsS http://127.0.0.1:8081/actuator/health > /dev/null 2>&1; do
        i=$((i + 1))
        [ "$i" -lt 60 ] || die "앱이 정상 상태가 되지 않았습니다. 'docker compose logs app'을 확인하세요"
        sleep 3
    done

    # 실행 중이 아닌 이미지는 지워 디스크가 차지 않게 한다.
    docker image prune -af > /dev/null
    echo "✓ 배포했습니다: ${image}"
}

import_catalog() {
    local data="${APP_DIR}/import-data"
    rm -rf "$data"
    install -d -m 755 "$data"
    cp -R "${UPLOAD_DIR}/bjcp" "${UPLOAD_DIR}/x-wines" "$data/"
    chmod -R a+rX "$data"
    rm -rf "$UPLOAD_DIR"

    compose run --rm import-catalog
    rm -rf "$data"
}

command="${1:-}"
shift || true
case "$command" in
    deploy) deploy "$@" ;;
    import) import_catalog ;;
    *) die "사용법: remote.sh deploy|import" ;;
esac
