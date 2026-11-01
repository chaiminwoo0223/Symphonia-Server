-- 기록 기간의 시작 시각은 도메인이 Clock의 시각으로 정하므로, JPA Auditing이 채우는 created_at과 분리한다(#162).
-- 이 변경 전에 연 모임은 created_at이 기록 기간의 시작 시각이었으므로 그 값으로 채운다.
ALTER TABLE gathering ADD COLUMN opened_at TIMESTAMP(6);
UPDATE gathering SET opened_at = created_at;
ALTER TABLE gathering ALTER COLUMN opened_at SET NOT NULL;
