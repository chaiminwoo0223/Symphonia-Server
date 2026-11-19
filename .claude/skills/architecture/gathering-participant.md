# gathering 참여자 상한과 모임장의 참여자 관리 규칙

`architecture` 스킬의 하위 문서다. 언제 읽는지는 `SKILL.md`의 하위 문서 표를 참고한다. 모임의 기본 모델과 정산 규칙은 `gathering.md`에 있다. 근거, 택하지 않은 안, 재검토 조건은 `docs/decisions/gathering.md`에 있다.

## 참여자 상한

- 한 모임의 참여자는 최대 20명(`GatheringPolicy.MAX_PARTICIPANT_COUNT`)이다. 모임을 만든 회원도 참여 화면으로 참여해야 한 명으로 센다.
- 참여자가 20명 이상이면 400(`GATHERING_PARTICIPANT_COUNT_EXCEEDS_LIMIT`)이고, 이미 참여한 휴대폰 검사 뒤에 검사한다. 이미 20명을 넘은 모임은 줄이지 않고 새 참여만 막는다.
- 참여 화면 조회는 가득 찬 모임을 참여 불가로 알린다.
- 서로 다른 휴대폰의 참여가 동시에 오면 인원 확인과 저장 사이에서 경합해 최종 인원이 20명을 넘을 수 있으므로, 20명은 보장된 상한이 아니다.

## 모임장의 참여자 관리

- 모임을 만든 회원은 JWT로 인증해 참여자 목록 조회(`GET /gatherings/{gatheringId}/participants`), 참여자 제거(`DELETE /gatherings/{gatheringId}/participants/{participantId}`), 개인 링크 재발급(`POST /gatherings/{gatheringId}/participants/{participantId}/link-token`)을 할 수 있다. `*Api`는 `GatheringApi`다.
- 세 요청 모두 파기 시각이 지난 모임을 404(`GATHERING_NOT_FOUND`)로, 모임을 만든 회원이 아니면 403(`NOT_GATHERING_HOST`)으로 거절한다. 모임에 없는 참여자는 404(`PARTICIPANT_NOT_FOUND`)다. 기록 기간 제한은 두지 않는다.
- 목록은 참여자의 id, 이름, 마스킹한 휴대폰을 담는다. 개인 링크 토큰은 담지 않는다.
- 재발급은 그 참여자의 기존 개인 링크 토큰을 모두 지우고 새 토큰을 발급해 응답에 한 번만 담는다. 201이다.
- 정산 건의 결제자나 분담금 인원인 참여자는 취소한 정산 건까지 포함해 제거할 수 없고 409(`PARTICIPANT_UNREMOVABLE`)다.
- 제거는 개인 링크 토큰, 계좌, 참여자를 한 트랜잭션에서 지우고 204다. 정산 건 기록과 동시에 와서 생기는 참조 제약 위반도 같은 409로 바꾼다.
- 제거한 휴대폰의 재참여는 막지 않는다.
