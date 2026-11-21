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
- 재발급은 모임 행을 `PESSIMISTIC_WRITE`로 잠그고 읽은 뒤, 그 참여자의 기존 개인 링크 토큰을 모두 지우고 새 토큰을 발급해 응답에 한 번만 담는다. 201이다. 같은 모임의 재발급이 겹치면 늦은 요청이 기다리므로 마지막에 발급한 토큰만 남는다. 파기의 `SKIP LOCKED`용 조회(`findByIdForUpdateSkipLocked`)는 기다리지 않으므로 재발급에 쓰지 않는다.
- 정산 건의 결제자나 분담금 인원인 참여자는 취소한 정산 건까지 포함해 제거할 수 없고 409(`PARTICIPANT_UNREMOVABLE`)다.
- 제거는 개인 링크 토큰, 피드백 제출 표시, 참여자를 한 트랜잭션에서 지우고 204다. 정산 건을 고쳐 참여자를 뺀 뒤에도 제출 표시는 남으므로 함께 지운다. 익명 피드백은 지우지 않는다. 정산 건 기록이나 링크 재발급과 동시에 와서 생기는 참조 제약 위반은 변환하지 않고 500으로 둔다.
- 제거한 휴대폰의 재참여는 막지 않는다.

## 모임장의 참여와 탈퇴

- 모임장이 탈퇴해도 모임은 자동 파기까지 유지한다. 탈퇴를 막지 않고 `GET /members/me/withdrawal-check`가 진행 중인 모임 수와 가장 늦은 파기 예정 시각을 알린다.
- 모임을 만든 회원은 `POST /gatherings/{gatheringId}/participants`로 JWT 인증해 참여 QR 없이 참여자가 된다. 이름, 휴대폰, 동의 여부, 기록 화면의 `consentVersion`을 보내고, 201과 함께 개인 링크 토큰을 한 번 돌려준다.
- 참여 기간, 동의, 휴대폰 중복, 인원 상한 검증은 QR 참여와 같다. 파기 시각이 지난 모임은 404(`GATHERING_NOT_FOUND`), 모임을 만든 회원이 아니면 403(`NOT_GATHERING_HOST`)이다.
- 참여자가 된 모임장이 다른 결제자의 정산 건 인원에 들어가면 이름, 전체 휴대폰, 분담 금액이 그 결제자의 수금 화면에 보인다. 직접 참여하지 않으면 정산 건 인원이나 결제자가 될 수 없다.
