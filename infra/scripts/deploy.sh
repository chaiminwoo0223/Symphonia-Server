#!/usr/bin/env bash
# 현재 커밋의 이미지를 빌드해 push하고 VM에서 실행한다. 배포하는 동안 앱이 잠시 중단된다.
source "$(dirname "$0")/common.sh"

[ -z "$(git -C "$ROOT" status --porcelain)" ] || die "커밋하지 않은 변경이 있습니다. 이미지 태그가 내용을 가리키도록 커밋한 뒤 실행하세요"

tf_init persistent
image="$(tf persistent output -raw image_repository):$(git -C "$ROOT" rev-parse --short HEAD)"
registry="${REGION}-docker.pkg.dev"

# 자격 증명이 로컬 Docker 설정에 남지 않게 push한 뒤 로그아웃한다.
trap 'docker logout "$registry" > /dev/null 2>&1 || true' EXIT
gcloud auth print-access-token | docker login -u oauth2accesstoken --password-stdin "$registry"
log "이미지를 빌드해 push합니다: ${image}"
# provenance 매니페스트는 태그 없는 항목으로 저장소에 쌓이므로 만들지 않는다.
docker buildx build --platform linux/amd64 --provenance=false --push -t "$image" "$ROOT"

wait_for_vm

work="$(mktemp -d)"
trap 'rm -rf "$work"; docker logout "$registry" > /dev/null 2>&1 || true' EXIT
tf persistent output -raw db_server_ca_cert > "${work}/server-ca.pem"

upload "${ROOT}/deploy/dev/compose.yaml" "${ROOT}/deploy/dev/Caddyfile" "${ROOT}/deploy/dev/remote.sh" \
    "${INFRA}/secrets.txt" "${work}/server-ca.pem"
vm_ssh "sudo bash /tmp/mariajou-upload/remote.sh deploy \
    '${PROJECT_ID}' '${image}' '$(tf persistent output -raw db_url)' '$(tf persistent output -raw db_username)' '${ACME_CA:-}'"
