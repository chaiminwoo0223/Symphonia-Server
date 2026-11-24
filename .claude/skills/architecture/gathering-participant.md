# gathering 참여자의 본인 확인, 상한, 모임장의 참여자 관리 규칙

`architecture` 스킬의 하위 문서다. 언제 읽는지는 `SKILL.md`의 하위 문서 표를 참고한다. 모임의 기본 모델과 정산 규칙은 `gathering.md`에 있다. 근거, 택하지 않은 안, 재검토 조건은 `docs/decisions/gathering.md`에 있다.

## 참여자의 본인 확인과 입력 규칙

- QR 참여(`POST /participants`)는 구글 ID 토큰(`googleIdToken`)을 필수로 받는다. 없으면 400, 유효하지 않으면 400(`INVALID_SOCIAL_TOKEN`)이다.
- `JoinGatheringService`(`@Service`)가 트랜잭션 밖에서 `auth`의 `VerifySocialTokenUseCase`로 검증하고 `sub`만 읽은 뒤, `JoinGatheringCommandService`(`@CommandService`)에 저장을 맡긴다. 검증 동안 DB 커넥션을 쥐지 않기 위해서다. 이메일, 이름, 사진, ID 토큰은 저장하지 않고 이메일도 보내지 않는다. 이메일이나 이름이 없는 토큰은 400(`SOCIAL_REQUIRED_INFO_MISSING`)이라 기본 로그인 범위로 로그인해야 한다. 구글 로그인은 본인 확인에만 쓰며 회원과 세션을 만들지 않는다. 검증 실패(400, 검증 장애 503)는 참여 토큰 404와 동의, 휴대폰 오류보다 먼저 나온다.
- 같은 구글 계정의 중복 참여는 휴대폰이 달라도 409(`PARTICIPANT_ALREADY_JOINED`)다. `participant.google_sub_hmac`에 모임별 HMAC(`"google:" + sub`)만 저장하고 `(gathering_id, google_sub_hmac)` unique 제약을 둔다. 같은 모임의 다른 참여자가 쓰는 휴대폰은 409(`PARTICIPANT_PHONE_NUMBER_ALREADY_USED`)다. 구글 계정과 휴대폰이 모두 겹치면 구글 계정 코드를 준다.
- 저장과 조회는 `ParticipantGoogleSubjectRepository`가 하고, 참여자를 저장한 뒤 값을 채운다. 모임 행 잠금 아래에서 검사하고, 동시 위반은 `ParticipantGoogleSubjectRepositoryImpl`이 `GatheringPolicy.validateGoogleAccountNotJoined`의 예외로 바꾼다.
- 모임장의 직접 참여(`POST /gatherings/{gatheringId}/participants`, JWT)는 구글 토큰 없이 참여하고 이 값은 NULL이다. 요청은 `JoinGatheringAsHostRequest`로 따로 받는다.
- 휴대폰은 010으로 시작하는 11자리만 받고(`PhoneNumber.from`) 하이픈은 지워 정규화한다. `+82`나 공백이 섞인 번호의 형식 정리는 클라이언트가 맡는다. 저장된 번호를 복원하는 `PhoneNumber` 생성자는 예전 번호도 복원하도록 기존 검증을 유지한다.
- 휴대폰은 본인이 입력한 값이고 인증하지 않는다. 이름은 앞뒤 공백을 제거해 저장한다(`Participant.of`).
- 참여 화면은 구글 계정 식별값(HMAC)을 모임 파기 때까지 보관한다는 목적과 보유 기간도 안내한다. 안내를 바꾸면 `GatheringPolicy.CONSENT_VERSION`도 바꾼다.

## 참여자의 개인 링크 복구

- 개인 링크를 잃은 참여자는 `POST /participants/link-token`(인증 없음)에 `X-Join-Token` 헤더와 구글 ID 토큰(`googleIdToken`)을 보내 새 링크를 받는다. 201이다. `*Api`는 `ParticipantApi`다.
- `RecoverParticipantLinkService`(`@Service`)가 트랜잭션 밖에서 `VerifySocialTokenUseCase`로 검증하고 `sub`만 읽은 뒤, `ParticipantCommandService`에 재발급을 맡긴다. 토큰 검증 실패는 참여와 같다.
- 모임은 참여 토큰으로 찾고, 파기 시각이 지난 모임은 404(`JOIN_TOKEN_NOT_FOUND`)다. 참여 토큰은 파기 때까지 남으므로 참여 기간이 끝난 뒤에도 복구에 쓸 수 있고, 모임장이 참여 QR을 재발급하면 옛 토큰으로는 복구할 수 없다.
- 모임 행을 `PESSIMISTIC_WRITE`로 잠그고 같은 `sub` HMAC의 참여자를 찾는다. 없으면 404(`PARTICIPANT_NOT_FOUND`)다.
- 기존 개인 링크 토큰을 모두 지우고 새 토큰을 응답에 한 번만 담는다. 모임장의 재발급과 같은 규칙이다.
- 구글 계정 없이 참여한 모임장과 구글 본인 확인 도입 전 참여자는 이 경로로 복구할 수 없고 모임장의 재발급을 쓴다.
- 같은 구글 계정으로 `POST /participants`를 다시 호출하면 계속 409(`PARTICIPANT_ALREADY_JOINED`)이고 링크를 발급하지 않는다. 클라이언트는 이 409를 받으면 링크 복구로 안내한다.

## 참여자의 이름과 휴대폰 정정

- 참여자는 `PUT /participants/profile`(인증 없음)로 본인의 이름과 휴대폰을 정정한다. `X-Join-Token` 헤더의 참여 토큰으로 모임을 찾고 본문 `googleIdToken`으로 본인을 확인하며 204다. 개인 링크는 필요하지 않고 바뀌지 않는다. `*Api`는 `ParticipantApi`다.
- 본문은 `name`, `phoneNumber`, `googleIdToken`을 모두 받는다. 현재 값과 같아도 된다.
- `CorrectParticipantService`(`@Service`)가 트랜잭션 밖에서 `VerifySocialTokenUseCase`로 검증하고 `sub`만 읽은 뒤, `ParticipantCommandService`에 정정을 맡긴다. 토큰 검증 실패는 참여와 같다.
- 참여 토큰이 없거나 파기 시각이 지난 모임이면 404(`JOIN_TOKEN_NOT_FOUND`), 같은 `sub`의 참여자가 없으면 404(`PARTICIPANT_NOT_FOUND`)다. 기록 기간 제한은 두지 않고 파기 시각 전까지 허용한다.
- 정산 건에 든 참여자도 정정할 수 있다. 정산 화면이 이름과 휴대폰을 매번 읽으므로 금액과 분담금 상태는 바뀌지 않는다.
- 휴대폰과 이름은 참여와 같은 규칙으로 검사하고 저장하며, 휴대폰 형식 오류는 400(`INVALID_PHONE_NUMBER`)이다.
- 같은 모임의 다른 참여자가 쓰는 휴대폰은 409(`PARTICIPANT_PHONE_NUMBER_ALREADY_USED`)이고 자기 휴대폰은 중복으로 보지 않는다. `Gathering.correctParticipant`가 검사한다.
- 모임 행을 `PESSIMISTIC_WRITE`로 잠그고 읽은 뒤 검사하고 저장한다. `GatheringRepository.save`는 기존 참여자 행을 다시 쓰지 않으므로 `updateParticipant`만 이름, 휴대폰, 휴대폰 HMAC을 바꾼다. 동시 위반은 `uk_participant_phone_number` 제약을 같은 409로 바꾼다.
- 구글 계정 없이 참여한 모임장과 구글 본인 확인 도입 전 참여자는 `sub`가 없어 정정할 수 없고 404다.

## 참여자 상한

- 한 모임의 참여자는 최대 20명(`GatheringPolicy.MAX_PARTICIPANT_COUNT`)이다. 모임을 만든 회원도 참여 화면으로 참여해야 한 명으로 센다.
- 참여자가 20명 이상이면 400(`GATHERING_PARTICIPANT_COUNT_EXCEEDS_LIMIT`)이고, 이미 참여한 휴대폰 검사 뒤에 검사한다. 이미 20명을 넘은 모임은 줄이지 않고 새 참여만 막는다.
- 참여 화면 조회는 가득 찬 모임을 참여 불가로 알린다.
- 참여는 모임 행을 `PESSIMISTIC_WRITE`로 잠그고 읽은 뒤 인원을 확인하고 저장한다(`findByIdForUpdate`). 잠금을 기다리는 동안 참여 기간이 끝날 수 있으므로 참여 기간은 잠금을 얻은 뒤 다시 읽은 시각으로 검사한다. 서로 다른 휴대폰의 참여가 동시에 와도 늦은 요청이 기다리므로 최종 인원은 20명을 넘지 않는다.

## 모임장의 참여자 관리

- 모임을 만든 회원은 JWT로 인증해 참여자 목록 조회(`GET /gatherings/{gatheringId}/participants`), 참여자 제거(`DELETE /gatherings/{gatheringId}/participants/{participantId}`), 개인 링크 재발급(`POST /gatherings/{gatheringId}/participants/{participantId}/link-token`)을 할 수 있다. `*Api`는 `GatheringApi`다.
- 세 요청 모두 파기 시각이 지난 모임을 404(`GATHERING_NOT_FOUND`)로, 모임을 만든 회원이 아니면 403(`NOT_GATHERING_HOST`)으로 거절한다. 모임에 없는 참여자는 404(`PARTICIPANT_NOT_FOUND`)다. 기록 기간 제한은 두지 않는다.
- 목록은 참여자의 id, 이름, 마스킹한 휴대폰을 담는다. 개인 링크 토큰은 담지 않는다.
- 재발급은 모임 행을 `PESSIMISTIC_WRITE`로 잠그고 읽은 뒤, 그 참여자의 기존 개인 링크 토큰을 모두 지우고 새 토큰을 발급해 응답에 한 번만 담는다. 201이다. 같은 모임의 재발급이 겹치면 늦은 요청이 기다리므로 마지막에 발급한 토큰만 남는다. 파기의 `SKIP LOCKED`용 조회(`findByIdForUpdateSkipLocked`)는 기다리지 않으므로 재발급에 쓰지 않는다.
- 취소하지 않은 정산 건의 결제자나 분담금 인원인 참여자는 제거하거나 철회할 수 없고 409(`PARTICIPANT_UNREMOVABLE`)다. 모임장이 정산 건을 고쳐 빼거나 취소한 뒤에 할 수 있다. 결제자는 정산 건에서 뺄 수 없어 취소해야 한다.
- 제거와 철회는 모임 행을 `PESSIMISTIC_WRITE`로 잠그고 읽은 뒤, 개인 링크 토큰, 피드백 제출 표시, 참여자를 한 트랜잭션에서 처리한다. 정산 건이 하나도 참조하지 않으면 참여자 행을 지우고, 취소한 정산 건만 참조하면 행을 익명화한다. 정산 건을 고쳐 참여자를 뺀 뒤에도 제출 표시는 남으므로 함께 지운다. 익명 피드백은 지우지 않는다. 제거는 204다.
- 익명화는 이름, 휴대폰, 휴대폰 HMAC, 구글 sub HMAC을 비우고 `withdrawn_at`을 남긴다. 익명화한 행은 모임을 읽을 때 불러오지 않아 참여자 목록, 인원 상한, 휴대폰 중복 검사, 모임 목록의 참여자 수에서 빠진다. 같은 구글 계정과 휴대폰의 재참여는 막지 않고 새 행으로 한다.
- 정산 건 기록, 수정, 취소도 모임 행을 `PESSIMISTIC_WRITE`로 잠그고 읽는다.

## 참여자의 철회

- 참여자는 `POST /participants/withdrawal`로 철회한다. `X-Join-Token` 헤더의 참여 토큰으로 모임을 찾고 본문 `googleIdToken`으로 본인을 확인하며 204다. 개인 링크는 필요하지 않다.
- 참여 토큰이 없거나 파기 시각이 지난 모임이면 404(`JOIN_TOKEN_NOT_FOUND`), 같은 구글 계정의 참여자가 없으면 404(`PARTICIPANT_NOT_FOUND`)다. 기록 기간 제한은 두지 않는다.
- `WithdrawParticipantService`(`@Service`)가 트랜잭션 밖에서 구글 ID 토큰을 검증하고, `RemoveParticipantBySubjectUseCase`에 철회를 맡긴다. 처리 규칙은 모임장의 제거와 같다.

## 모임장의 참여와 탈퇴

- 모임장이 탈퇴해도 모임은 자동 파기까지 유지한다. 탈퇴를 막지 않고 `GET /members/me/withdrawal-check`가 진행 중인 모임 수와 가장 늦은 파기 예정 시각을 알린다.
- 모임을 만든 회원은 `POST /gatherings/{gatheringId}/participants`로 JWT 인증해 참여 QR 없이 참여자가 된다. 이름, 휴대폰, 동의 여부, 기록 화면의 `consentVersion`을 보내고, 201과 함께 개인 링크 토큰을 한 번 돌려준다.
- 참여 기간, 동의, 휴대폰 중복, 인원 상한 검증은 QR 참여와 같다. 파기 시각이 지난 모임은 404(`GATHERING_NOT_FOUND`), 모임을 만든 회원이 아니면 403(`NOT_GATHERING_HOST`)이다. 이미 다른 참여자가 쓰는 휴대폰은 409(`PARTICIPANT_PHONE_NUMBER_ALREADY_USED`)다.
- 참여자가 된 모임장이 다른 결제자의 정산 건 인원에 들어가면 이름, 전체 휴대폰, 분담 금액이 그 결제자의 수금 화면에 보인다. 직접 참여하지 않으면 정산 건 인원이나 결제자가 될 수 없다.
