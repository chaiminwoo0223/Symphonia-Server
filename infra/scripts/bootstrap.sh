#!/usr/bin/env bash
# Terraform 상태 버킷을 만든다. 상태를 담는 버킷은 Terraform이 관리하지 않고, teardown.sh가 마지막에 지운다.
source "$(dirname "$0")/common.sh"

gc services enable cloudresourcemanager.googleapis.com serviceusage.googleapis.com storage.googleapis.com

if ! bucket_exists; then
    log "상태 버킷을 만듭니다: gs://${STATE_BUCKET}"
    gc storage buckets create "gs://${STATE_BUCKET}" \
        --location="$REGION" \
        --uniform-bucket-level-access \
        --public-access-prevention \
        --soft-delete-duration=0
fi

# 상태를 되돌릴 수 있게 버전을 켜되, 이전 버전은 3개까지만 남긴다.
lifecycle="$(mktemp)"
trap 'rm -f "$lifecycle"' EXIT
echo '{"rule":[{"action":{"type":"Delete"},"condition":{"numNewerVersions":3}}]}' > "$lifecycle"
gc storage buckets update "gs://${STATE_BUCKET}" --versioning --lifecycle-file="$lifecycle"
