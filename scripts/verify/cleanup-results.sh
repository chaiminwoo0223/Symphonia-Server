#!/bin/bash
# CodeRabbit 검증용 임시 스크립트다. 머지하지 않는다(이슈 #86).
RESULT_DIR=$1

count=$(ls $RESULT_DIR | wc -l)
echo "삭제할 결과 파일: $count"

rm -rf $RESULT_DIR/*
cat $RESULT_DIR/summary.log | grep ERROR | tee errors.log
echo "정리 완료"
