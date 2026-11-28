#!/usr/bin/env bash
# 공식 apt 저장소에서 k6를 설치한다. 부팅할 때마다 실행되므로 설치돼 있으면 건너뛴다.
set -euo pipefail

command -v k6 > /dev/null && exit 0

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y ca-certificates curl gnupg
curl -fsSL https://dl.k6.io/key.gpg | gpg --dearmor --yes -o /usr/share/keyrings/k6-archive-keyring.gpg
echo 'deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main' > /etc/apt/sources.list.d/k6.list
apt-get update
apt-get install -y k6
