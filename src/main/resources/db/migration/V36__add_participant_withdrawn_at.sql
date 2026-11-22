-- 취소한 정산 건의 분담금 행이 참여자 행을 참조하므로, 그 정산 건에만 든 참여자가 철회하거나 제거되면 행을 지우지 않고 익명화한다(#221).
-- 익명화한 행은 이름, 휴대폰, 구글 계정 값을 비우고 철회 시각만 남기며, 모임을 파기할 때 다른 참여자 행과 함께 지워진다.
ALTER TABLE participant ADD COLUMN withdrawn_at TIMESTAMP(6);

ALTER TABLE participant ALTER COLUMN encrypted_name DROP NOT NULL;
ALTER TABLE participant ALTER COLUMN encrypted_phone_number DROP NOT NULL;
ALTER TABLE participant ALTER COLUMN phone_number_hmac DROP NOT NULL;

-- 익명화하지 않은 참여자는 이전처럼 세 값을 모두 가져야 한다.
ALTER TABLE participant ADD CONSTRAINT ck_participant_withdrawn CHECK (
    withdrawn_at IS NOT NULL
    OR (encrypted_name IS NOT NULL AND encrypted_phone_number IS NOT NULL AND phone_number_hmac IS NOT NULL));
