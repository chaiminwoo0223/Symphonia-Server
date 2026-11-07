-- 정산 페이지의 참여자 피드백은 회원 id 없이 익명으로 남긴다(#131).
-- member_id가 NULL인 행은 PostgreSQL이 서로 다른 값으로 보므로 uk_pairing_feedback_member_combination에 걸리지 않는다.
ALTER TABLE pairing_feedback ALTER COLUMN member_id DROP NOT NULL;
