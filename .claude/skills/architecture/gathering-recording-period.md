# gathering 기록 기간 연장 규칙

`architecture` 스킬의 하위 문서다. 언제 읽는지는 `SKILL.md`의 하위 문서 표를 참고한다. 기록 기간의 기본 규칙(48시간, `GatheringPolicy` 판정)은 `gathering.md`에 있다. 근거, 택하지 않은 안, 재검토 조건은 `docs/decisions/gathering.md`에 있다.

## 기록 기간의 시작과 연장

- 기록 기간의 시작 시각은 `Gathering.openedAt`이다. `Gathering.open`이 Service가 `Clock`으로 구한 현재 시각을 받아 담는다. 감사용 `created_at`과 별도인 `opened_at` 컬럼이다.
- 모임을 만든 회원이 JWT로 `PUT /gatherings/{gatheringId}/recording-extension`을 호출해 기록 기간을 한 번 48시간 연장한다. 연장하면 정산 건 기록, 차수 추가, 참여, 참여 토큰 재발급, 페어링 변경이 같은 기간을 따른다.
- 기록 기간이 끝난 모임은 400(`GATHERING_RECORDING_EXTENSION_CLOSED`)이다. 이미 연장한 모임에 다시 요청하면 바꾸지 않고 200으로 `recordingClosesAt`을 돌려준다.
- 연장 여부는 `gathering.recording_extended`이고 `GatheringRepository.markRecordingExtended`로만 바꾼다. 모임 저장은 이 컬럼을 바꾸지 않는다.
- 파기 시각의 완료 하한은 연장한 기록 기간이 끝나는 시각이고 29일 상한은 그대로다. 파기 스윕의 후보 기준은 가장 짧은 기록 기간(48시간)이고, 연장한 모임은 파기 시각 판단이 거른다.
- 기록 화면 응답은 `recordingClosesAt`, `recordingExtended`, 모임장이 직접 참여할 때 보낼 `consentVersion`을 담는다.
