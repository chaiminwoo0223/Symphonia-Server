#!/usr/bin/env bash
# k6 VM을 만들거나 지운다. 인자는 up 또는 down이다.
source "$(dirname "$0")/common.sh"

case "${1:-}" in
    up)
        tf_init loadgen
        tf loadgen apply
        ;;
    down)
        tf_init loadgen
        tf loadgen destroy
        ;;
    *) die "사용법: loadgen.sh up|down" ;;
esac
