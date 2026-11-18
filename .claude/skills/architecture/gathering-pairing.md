# gathering 모임 페어링과 익명 피드백 규칙

`architecture` 스킬의 하위 문서다. 언제 읽는지는 `SKILL.md`의 하위 문서 표를 참고한다. 모임의 기본 모델과 정산 규칙은 `gathering.md`에 있다. 근거, 택하지 않은 안, 재검토 조건은 `docs/decisions/gathering.md`에 있다.

- 모임 페어링은 모임을 열 때 선택값으로 받는다. 모임장은 기록 기간 안에 `PUT /api/v1/gatherings/{gatheringId}/pairing`으로 고르거나 바꾸고, 기간이 지나면 400(`GATHERING_PAIRING_CLOSED`)이다.
- 페어링은 `gathering_pairing` 테이블에 id로만 둔다. FK는 걸지 않고, 고를 때 `VerifyCatalogReferencesUseCase`로 대상이 있는지 확인한다.
- 참여자는 정산 페이지에서 평점만 고른다. 관계와 분위기는 모임 페어링의 값을 쓴다.
- 피드백 요청은 참여자가 본 술과 안주의 id(`drinkId`, `anjuId`)를 함께 담는다. 모임의 페어링과 다르면 제출 표시를 남기기 전에 409(`GATHERING_PAIRING_CONFLICT`)로 거절한다.
- 정산에서 돈이 오간 참여자만 피드백을 낼 수 있다. 취소하지 않은 정산 건에서 0원이 아닌 본인 분담금이 보냄이나 확인 완료인 참여자와, 다른 참여자가 0원이 아닌 분담금을 보냄이나 확인 완료로 낸 결제자다. 0원 분담금의 참여자는 낼 수 없다. 아니면 400(`SENT_SHARE_REQUIRED`)이다.
- 검사 순서는 모임 페어링 조회, 대상 조합 일치, 낼 수 있는 참여자, 이미 낸 참여자 순이다. 이미 낸 참여자가 그 사이 모임의 페어링이 바뀌었거나 낼 조건을 잃었다면 이미 냈다는 409 대신 그 오류를 받는다.
- 피드백은 익명이다. `pairing_feedback.member_id`는 nullable이고, 모임을 만든 회원의 피드백도 익명으로 받는다. 피드백은 참여자를 참조하지 않는다.
- 피드백은 참여자마다 한 번만 받는다. 제출 표시는 `participant_pairing_feedback`에 `ON CONFLICT DO NOTHING`으로 넣고, 넣지 못하면 409(`PAIRING_FEEDBACK_ALREADY_SUBMITTED`)다. 제출 표시는 참여자와 함께 파기된다. 평점은 바꿀 수 없다.
- 정산 페이지는 `GET /api/v1/participants/pairing`으로 술과 안주의 id와 이름, 제출 여부, 지금 낼 수 있는지(`feedbackSubmittable`)를 받는다. 제출한 참여자는 정산 건을 읽지 않고 낼 수 없다고 알린다. 안주 id는 정산 페이지가 맛집 조회(`place`)에 쓴다. 이름은 읽을 때 catalog의 `GetCatalogReferencesUseCase`로 조회하고, 모임에 복사해 두지 않는다. 고른 페어링이 없거나 카탈로그에서 대상이 사라졌으면 404다.
- 피드백 제출은 `gathering`의 `*Service`가 pairing의 `SubmitAnonymousPairingFeedbackUseCase`를 직접 의존해 처리한다. 제출 표시와 익명 피드백 저장은 한 트랜잭션에 둔다.
- `SubmitPairingFeedbackUseCase`, `SubmitAnonymousPairingFeedbackUseCase`가 같은 트랜잭션에서 부르는 `VerifyCatalogReferencesUseCase`의 NotFound 예외는 잡지 않는다. 잡으면 바깥 트랜잭션이 rollback-only라 커밋할 때 `UnexpectedRollbackException`이 난다.
