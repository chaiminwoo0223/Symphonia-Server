# gathering 정산 규칙

`architecture` 스킬의 하위 문서다. 언제 읽는지는 `SKILL.md`의 하위 문서 표를 참고한다. 모임 페어링과 익명 피드백 규칙은 `gathering-pairing.md`, 참여자 상한과 모임장의 참여자 관리 규칙은 `gathering-participant.md`에 있다. 근거, 택하지 않은 안, 재검토 조건은 `docs/decisions/gathering.md`에 있다.

## 애그리거트와 application

- 도메인 하나에 애그리거트 `Gathering`(참여자와 차수 포함)과 `Settlement`(분담금 포함) 두 개를 둔다. 정산 건 하나는 카드 결제 1건이다. 정산 건끼리 상계하지 않는다.
- 참여자는 `Member`가 아니라 `Participant`로 모델링하고 `Gathering`에 둔다. 모임을 만든 회원도 참여자로 들어간다. `Participant`는 `Member`를 참조하지 않고, `Gathering`만 모임을 만든 회원의 id를 갖는다. 회원도 참여 화면에서 이름과 휴대폰을 따로 입력한다.
- 모임은 회원이 입력한 이름을 갖는다.
- application은 흐름 단위로 나눈다. 정산 건 기록, 수정, 취소는 `SettlementCommandService`, 분담금 상태 변경은 `ShareCommandService`가 맡는다.
- 개인 링크 토큰으로 참여자를 찾는 경로는 `GetLinkedParticipantUseCase` 하나로 모은다. `GetLinkedParticipantUseCase` 구현 밖의 Service는 링크 토큰 Repository를 직접 부르지 않는다.
- 참여 기간, 휴대폰 중복, 모임을 만든 회원 여부, 동의 검증은 `GatheringPolicy`에 두고, Service가 `Gathering`의 상태를 바꾸기 전에 호출한다. `Gathering`은 검증을 통과한 값으로 상태만 바꾼다. Service 테스트가 검증 실패 경우를 하나씩 확인한다.
- 48시간 기록 기간 판정은 `GatheringPolicy`에 두고 `Settlement`도 같이 쓴다.
- 48시간 기록 기간의 시작 시각은 `Gathering.openedAt`이다. `Gathering.open`이 Service가 `Clock`으로 구한 현재 시각을 받아 담는다. 감사용 `created_at`과 별도인 `opened_at` 컬럼이다.

## 차수

- 차수(`GatheringRound`)는 술자리 단위이고 `Gathering`이 가진다. 정산 건은 차수 하나에 속하고, 한 차수에 정산 건이 여러 개일 수 있다.
- 차수 번호는 1부터 빈틈없이 이어진다. 차수는 지우지 않는다.
- `Gathering.open`이 모임을 열 때 1차를 만든다.
- 모임을 만든 회원이 JWT로 다음 차수를 추가한다(`POST /gatherings/{gatheringId}/rounds`, 본문 `roundNumber`). `*Api`는 `GatheringApi`다. 파기 시각이 지난 모임은 404(`GATHERING_NOT_FOUND`)로, 모임을 만든 회원이 아니면 403(`NOT_GATHERING_HOST`)으로 거절한다.
- 요청한 번호의 차수가 이미 있으면 새로 만들지 않고 그 차수를 돌려준다. 마지막 번호의 다음 번호보다 큰 번호는 400(`INVALID_GATHERING_ROUND_NUMBER`)으로 거절한다.
- 동시 추가는 `(gathering_id, round_number)` unique 제약과 `ON CONFLICT DO NOTHING`으로 막고, 넣지 못하면 이미 있는 차수를 읽어 돌려준다.
- 모임의 차수는 최대 10개(`GatheringPolicy.MAX_ROUND_COUNT`)이고, 상한을 넘는 번호는 건너뛴 번호보다 먼저 400(`GATHERING_ROUND_COUNT_EXCEEDS_LIMIT`)으로 거절한다.
- 차수 추가는 모임 생성 후 48시간 안에만 할 수 있다. 지나면 400(`GATHERING_ROUND_CLOSED`)이다.
- 차수에는 인원 명단을 두지 않는다. 인원은 정산 건이 가진다.

## 참여와 동의

- 참여자는 이름과 휴대폰을 필수로 입력한다. 술을 마시는지는 받지 않는다. 휴대폰은 결제자가 참여자를 카카오톡 방에 초대해 정산하는 목적으로 수집하고, 중복 참여 방지에 쓴다. 응답에는 마스킹해서 보여 주고, 결제자의 수금 화면에만 전체 번호를 담는다.
- 결제자의 계좌는 받지 않는다. 결제자들이 카카오톡 방에서 정산하고, 받은 돈은 결제자가 직접 받음(`confirm`)으로 처리한다.
- 참여 화면에서는 수집 항목, 목적, 보유 기간(최대 30일), 동의를 거부할 권리와 불이익을 안내하고 필수 동의를 받는다. 참여 화면은 같은 차수의 참여자에게 이름이 보인다는 사실도 목적으로 안내한다.
- 참여자는 동의 시각과 안내문 버전(`consent_version`)을 남긴다. 버전의 단일 소스는 `GatheringPolicy.CONSENT_VERSION`이고, 참여 화면 조회 응답에 담는다. 안내 문구를 바꾸면 이 상수도 바꾼다.
- 참여 요청은 동의 여부와 버전을 함께 보낸다. `GatheringPolicy.validateConsent`가 동의하지 않았으면 `ConsentRequiredException`(400), 버전이 다르면 `OutdatedConsentVersionException`(400)으로 거절한다. Request의 Bean Validation으로 검사하지 않는다.
- 같은 휴대폰의 중복 참여를 막는다. 동시 참여는 `(gathering_id, phone_number_hmac)` unique 제약으로 막고, `GatheringRepositoryImpl`이 위반을 `GatheringPolicy.validateNotJoined`의 예외로 바꾼다.
- 참여 화면은 참여 토큰으로 모임 이름, 참여 가능 여부(`joinable`), 사유(`joinClosedReason`: `PERIOD_EXPIRED`, `PARTICIPANT_LIMIT_REACHED`)를 먼저 조회한다. 둘 다면 `PERIOD_EXPIRED`다.
- 참여 토큰은 모임 생성 후 48시간까지 유효하다. 모임을 만든 회원은 참여 QR을 재발급할 수 있고, 재발급하면 기존 토큰은 무효다. 참여 기간이 끝난 모임은 재발급하지 않는다.
- 참여 토큰 발급 응답은 참여 기간이 끝나는 `expiresAt`을 담는다.
- 모임장이 탈퇴해도 모임은 자동 파기까지 유지한다.
- `GatheringRepositoryImpl.save`는 id가 없는 새 참여자만 INSERT하고 기존 참여자 행을 다시 쓰지 않는다.

## 분담 방식

- 분담 방식은 똑같이 나누기 하나다. 결제자는 다른 참여자의 금액을 직접 정할 수 있고, 나머지 참여자는 결제자와 함께 남은 금액을 똑같이 나눈다. 결제자 본인의 금액은 지정할 수 없고, 총액에서 다른 참여자의 분담액 합을 뺀 값이다.
- 분담 방식은 값 객체(`SplitMethod`)이고, 만들 때 불변식(지정 금액 합 ≤ 총액, 똑같이 나눌 참여자 한 명 이상, 결제자 분담액 ≥ 0)을 검사한다. 지정 금액에 다른 참여자 몫보다 작아야 한다는 조건은 두지 않는다.
- 똑같이 나누는 분담액은 1원 단위로 내리고, 끝전은 결제자 분담액에 남긴다. 지정 금액은 바꾸지 않는다. 결제자 분담액을 따로 보정하지 않는다.
- 반올림 정책 `ShareRoundingPolicy`는 분담 방식과 분리하고, `SplitMethod`가 직접 호출한다.
- 안 마신 분 정액, 술값 분리, 영수증 항목별 분담, 영수증 OCR은 두지 않는다. 일부만 마신 비싼 술은 마신 사람만으로 정산 건을 따로 기록한다.
- 영수증 사진은 서버가 받지 않는다. 금액은 결제자가 직접 입력한다.

## 정산 건 기록, 수정, 취소

- 정산 건은 모임을 만든 회원이 JWT로 기록, 수정, 취소한다. 결제자는 기록할 때 요청 본문의 `payerParticipantId`로 지정하고, 그 모임의 참여자가 아니면 404(`PARTICIPANT_NOT_FOUND`)다. 모임장은 참여자가 아니어도 된다. 결제자는 수정으로 바꿀 수 없고, 바꾸려면 취소하고 다시 기록한다.
- 기록, 수정, 취소는 파기 시각이 지난 모임을 404(`GATHERING_NOT_FOUND`)로, 모임을 만든 회원이 아니면 403(`NOT_GATHERING_HOST`)으로 거절한다. 다른 모임의 정산 건은 404(`SETTLEMENT_NOT_FOUND`)다.
- 정산 건은 모임 생성 후 48시간 안에만 기록할 수 있다.
- 정산 건 기록 요청은 클라이언트가 만든 요청 id(`clientRequestId`, UUID)를 필수로 담는다. 같은 결제자가 같은 요청 id로 다시 보내면 새로 만들지 않고 처음 기록한 정산 건을 201로 돌려주고, 요청 내용이 달라도 처음 정산 건을 돌려준다. 이미 있는 정산 건은 차수, 인원, 기록 기간 검증보다 먼저 찾아 그대로 돌려준다.
- 요청 id는 `settlement`의 `(payer_participant_id, client_request_id)` unique 제약과 `ON CONFLICT DO NOTHING`으로 가린다. 동시에 같은 요청이 오면 늦은 요청은 먼저 넣은 요청이 커밋한 뒤 그 정산 건을 읽는다.
- 정산 건은 차수 id를 지정해 기록한다. 그 모임의 차수가 아니면 404(`GATHERING_ROUND_NOT_FOUND`)다. 보낸 분담금이 없는 정산 건은 수정할 때 차수도 바꿀 수 있다.
- 인원 기본값은 차수와 관계없이 직전 정산 건의 인원이고, 첫 건은 참여자 전원이다.
- 보낸 분담금이 있는 정산 건은 수정을 막는다(`SettlementSharesSentException`).
- 취소는 기록 기간이 지나도 할 수 있다. 보낸 분담금이 있는 정산 건은 기간과 관계없이 취소할 수 없다. 결제자가 먼저 미입금으로 되돌리고 받은 돈은 직접 돌려준다.
- 결제자가 더 내는 경우는 더 낼 금액만큼 총액을 줄여 기록한다.
- 기록 화면 조회는 모임의 취소하지 않은 정산 건을 함께 돌려준다. 참여자에는 마스킹한 휴대폰을, 정산 건에는 확인 완료 금액과 보냄 금액과 자기 차수의 id와 번호를 담는다. 정산 건이 없는 빈 차수를 포함한 차수 목록(id, 번호)도 함께 담는다.
- 기록 화면 응답에는 그 정산 건들의 총액 합계(`totalAmountSum`)와 받을 금액 합계(`receivableAmountSum`)도 담는다. 받을 금액은 총액에서 결제자 분담액을 뺀 값이다. 취소나 재기록 뒤에는 클라이언트가 기록 화면을 다시 조회한다.
- 인원 규칙 위반은 모두 `INVALID_SETTLEMENT_PARTICIPANTS` 하나로 알린다.

## 동시성

- `Settlement`는 애그리거트 버전(`version`)을 domain 모델에 갖고, `settlement.version`으로 낙관적 락을 건다. 분담금만 바뀌어도 버전을 올린다.
- 기존 정산 건은 저장할 때마다 `PESSIMISTIC_FORCE_INCREMENT`로 버전을 바로 올린다. 분담금 행만 UPDATE하는 저장 경로를 따로 두지 않는다.
- `SettlementRepositoryImpl`은 `saveAndFlush`로 저장하고, 버전 충돌을 `SettlementConflictException`(409)으로 바꾼다.
- "보냈어요"는 그 결제자의 정산 건을 id 순으로 행 잠금하고 읽는다. 취소나 수정과 겹치면 버전 충돌로 막는다.
- 정산 API는 모임을 읽을 때 참여자 전원의 이름과 휴대폰을 복호화한다. 일부만 채운 `Gathering`을 만들지 않는다.

## 분담금 상태

- 참여자가 결제자에게 낼 몫은 분담금(share)이라 부른다. 결제(payment)는 정산 건을 가리킨다. 상태 enum은 `ShareStatus`, 컬럼은 `settlement_share.status`다.
- 상태는 미입금, 보냄, 확인 완료 세 가지다. 분담금은 미입금으로 시작하고, 0원 분담금은 확인 완료로 시작한다.
- 참여자가 개인 링크에서 "보냈어요"를 누르면 그 결제자에게 낼 미입금 분담금이 모두 보냄이 된다. 이미 모두 보냈으면 바꾸지 않고 성공을 돌려준다. 참여자가 직접 취소하는 기능은 두지 않는다.
- 결제자는 필요할 때만 정정한다. 확인 완료로 바꾸거나(직접 받음 포함, `confirm`), 미입금으로 되돌린다(`unpaid`). 확인 완료도 미입금으로 되돌릴 수 있다.
- 0원 분담금은 결제자가 상태를 바꿀 수 없고, 보낸 분담금으로 보지 않는다. 0원을 잘못 지정했으면 정산 건을 수정한다.
- 상태 변경 규칙은 `Settlement`가 판단한다. `SettlementShare`의 상태 변경 메서드는 `public`이고, 상태가 바뀐 새 분담금을 돌려준다. `Settlement`가 그 분담금으로 목록을 갈아 끼운다.
- 같은 상태로 바꾸라는 요청을 다시 보내면 바꾸지 않고 성공으로 돌려준다. 이미 확인 완료인 분담금의 확인 완료, 이미 미입금인 분담금의 되돌리기, 이미 취소한 정산 건의 취소가 해당한다. 이때 상태를 바꾼 시각, 취소 시각, 버전은 그대로 두고 정산 건을 저장하지 않는다.
- 바꿀 수 없는 상태 변경은 0원 분담금과 취소한 정산 건의 변경으로 나눠 각각 다른 예외(409)로 알린다. `SHARE_ALREADY_SENT`와 `SHARE_ALREADY_CONFIRMED`는 `SettlementShare.sent`의 방어 분기로만 남고 API로는 나오지 않는다.
- 분담금은 상태를 바꾼 시각(`settlement_share.status_changed_at`)을 갖는다.
- 상태를 바꾸는 API는 바뀐 정산 건이나 결제자 묶음을 돌려준다.

## 개인 링크 화면

- 개인 링크는 취소하지 않은 정산 건의 본인 분담금을 상태와 함께 모두 보여 주고, 미입금이 남은 결제자를 앞에 둔다.
- 계산 근거는 총액, 결제자를 포함한 인원, 본인 금액의 지정 여부, 본인 금액, 정산 건의 차수(id, 번호), 그 차수의 참여자 이름이다. 참여자 이름은 그 차수의 취소하지 않은 정산 건 인원의 합집합이다. 다른 참여자의 분담액, 남은 금액, 지정 금액의 합, 휴대폰은 보여 주지 않는다.
- 결제자의 수금 화면(`GET /settlements`, `X-Link-Token`)은 그 결제자가 기록된 취소하지 않은 정산 건을 참여자별로 묶어 돌려준다. 참여자의 이름, 전체 휴대폰, 정산 건별 차수와 받을 금액과 상태(정산 건 id 포함), 참여자별 합계, 전체 합계, `deletesAt`을 담는다. 0원 분담금의 참여자도 담고, 결제자로 지정된 정산 건이 없으면 빈 목록이다. 파기 시각이 지난 모임은 다른 링크 토큰 API와 같이 404(`LINK_TOKEN_NOT_FOUND`)다.
- 참여자 화면에는 결제자의 휴대폰을 보여 주지 않는다.
- 결제자 현황은 마이페이지가 아니라 개인 링크의 수금 화면에 둔다.

## 개인정보 암호화와 토큰

- 참여자의 이름과 휴대폰은 모임별 데이터 키로 AES-GCM 암호화한다. 데이터 키는 마스터 키로 암호화해 저장하고, 마스터 키는 `${ENV_VAR}`로 주입한다.
- 모임별 데이터 키는 암호화 키와 휴대폰 HMAC 키로 이루어지고 `gathering_data_key` 테이블에 둔다.
- 암호화와 복호화는 `ParticipantJpaEntity`가 데이터 키를 받아 `from()`과 `toDomain()`에서 한다. `Participant`는 평문 값 객체를 갖는다.
- 참여 토큰과 개인 링크 토큰은 128비트 이상 무작위 값이고, DB에는 해시만 `gathering_join_token`, `participant_link_token` 테이블에 저장한다. 해싱은 `GatheringJoinTokenRepositoryImpl`, `ParticipantLinkTokenRepositoryImpl`이 하고, domain과 application은 원본 토큰만 다룬다.
- 개인 링크 토큰은 참여 응답에서 발급하고, 모임을 파기할 때까지 유효하다. `participant_link_token`은 `participant_id`에 인덱스를 둔다.
- 휴대폰 번호만으로 링크를 다시 주지 않는다.
- 응답의 휴대폰은 마스킹하고 결제자의 수금 화면만 예외로 한다. 로그와 예외 메시지에는 개인정보를 넣지 않는다.

## 파기

- 모임이 끝난 시각은 미입금 분담금이 없을 때 기록 기간 종료, 분담금 상태 변경, 정산 건 취소 중 가장 늦은 시각이다. 끝나고 7일이 지나면 모임 전체를 파기한다. 모임 생성 29일 뒤에는 정산 완료 여부와 관계없이 파기 대상이다.
- 파기 시각은 `GatheringRetention`이 계산하고, `GatheringRetentionRepository`가 참여자를 복호화하지 않고 읽는다.
- 파기 대상 모임은 물리 삭제 전에도 기존 404(`LINK_TOKEN_NOT_FOUND`, `JOIN_TOKEN_NOT_FOUND`, `GATHERING_NOT_FOUND`)로 응답하고, 복호화 전에 검사한다.
- 파기는 1시간 주기 `@Scheduled` 스윕이다. 스윕은 생성 9일이 지난 모임을 잠그지 않고 정산 건, 데이터 키와 함께 읽어 파기 시각이 지난 모임만 고르고, 판단은 Java에서만 한다. 그 모임마다 트랜잭션을 연다. 묶음으로 파기하지 않는다.
- 모임마다 두 트랜잭션으로 파기한다. 1단계는 모임을 `SKIP LOCKED`로 잠그고 정산 건을 잠근 뒤 파기 시각을 다시 판단해 데이터 키를 지우고 커밋한다. 2단계는 링크 토큰, 피드백 제출 표시, 분담금과 정산 건, 참여 토큰, 모임 페어링, 차수, 참여자와 모임 순서로 테이블마다 쿼리 한 번씩 지운다.
- 데이터 키가 없는 모임은 파기가 확정된 것으로 보고, 2단계가 실패하면 다음 스윕이 2단계만 다시 한다.
- 모임 조회, 기록 화면, 개인 링크 응답에 파기 예정 시각(`deletesAt`)을 담는다. `GET /shares` 응답은 `{ deletesAt, payers }`다.

## API 경로와 Api 분리

- 참여 토큰과 개인 링크 토큰은 경로가 아니라 `X-Join-Token`, `X-Link-Token` 헤더로 받는다.
- 경로는 자원 중심으로 둔다. 모임 열기는 `POST /gatherings`, 참여는 `POST /participants`, 정산 건 기록과 수정은 `POST /gatherings/{gatheringId}/settlements`와 `PUT /gatherings/{gatheringId}/settlements/{settlementId}`다. 모임장 전용 경로는 `/gatherings/{gatheringId}` 아래에 두고 `GatheringApi`가 맡는다. HTTP 메서드와 뜻이 겹치는 동사(`open`, `record`, `update`)는 경로에 쓰지 않는다.
- 링크 토큰으로 부르는 API는 경로에 본인을 적지 않는다. `me`는 JWT로 인증한 회원(`/members/me`)에만 쓴다.
- 취소는 `POST /gatherings/{gatheringId}/settlements/{settlementId}/cancel`, 기록 화면 조회는 `GET /gatherings/{gatheringId}/settlements/form`, 참여 토큰 재발급은 `POST /gatherings/{gatheringId}/join-token`이다. 페어링 추천은 `GET /pairings/recommendations`다. 인증 API(`/auth/login` 등)는 동사 경로를 유지한다.
- 참여자 쪽 분담금 API는 `ShareApi`다. 조회는 `GET /shares`, "보냈어요"는 `POST /shares/sent`이고, 결제자는 요청 본문의 `payerParticipantId`로 고른다. 결제자의 정정은 `POST /settlements/{settlementId}/shares/{participantId}/confirm`과 `/unpaid`다.
- `*Api`와 `*Controller`는 애그리거트가 아니라 경로의 자원 단위로 나눈다. `*Api`와 `*Endpoints`는 1:1이다(예: `/participants` 아래 참여와 페어링 피드백은 `ParticipantApi`).

## 에러 코드

- 토큰을 찾지 못하면 참여자나 모임이 아니라 토큰을 찾지 못했다고 알린다(`LINK_TOKEN_NOT_FOUND`, `JOIN_TOKEN_NOT_FOUND`). 토큰은 찾았지만 모임이 없으면 `GATHERING_NOT_FOUND`다.
- 사용자가 겪는 상황마다 예외를 따로 둔다(예: `SETTLEMENT_ALREADY_CANCELLED`). 화면이 막을 수 있는 클라이언트 버그는 하나로 묶는다(예: `INVALID_SETTLEMENT_PARTICIPANTS`). 404 코드는 `*_NOT_FOUND`로 짓는다.
