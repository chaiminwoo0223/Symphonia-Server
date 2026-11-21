-- 참여자가 구글 로그인으로 본인을 확인한 계정을 모임 안에서 한 번만 참여하게 하려고 구글 sub의 HMAC을 둔다(#206).
-- sub 원문은 저장하지 않는다. 모임별 데이터 키로 계산한 HMAC만 저장하고, 모임을 파기하면 참여자 행과 함께 지워진다.
-- 모임장이 직접 참여한 행과 기존 참여자는 구글 계정이 없어 NULL로 둔다.
ALTER TABLE participant ADD COLUMN google_sub_hmac VARCHAR(64);

-- NULL은 서로 같지 않아 구글 계정이 없는 참여자끼리 충돌하지 않는다.
ALTER TABLE participant ADD CONSTRAINT uk_participant_google_sub UNIQUE (gathering_id, google_sub_hmac);
