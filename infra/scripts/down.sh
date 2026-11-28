#!/usr/bin/env bash
# k6 VM과 앱 VM을 지운다. DB, 고정 IP, 이미지, 비밀값은 남는다. Redis의 로그인 정보는 VM과 함께 사라진다.
source "$(dirname "$0")/common.sh"

for module in loadgen app; do
    tf_init "$module"
    if has_resources "$module"; then
        log "${module} 모듈을 지웁니다"
        tf "$module" destroy
    fi
done
