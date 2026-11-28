#!/usr/bin/env bash
# pause.sh로 중지한 DB와 VM을 다시 시작한다. 컨테이너는 재시작 정책에 따라 자동으로 올라온다.
source "$(dirname "$0")/common.sh"

resume_if_stopped
