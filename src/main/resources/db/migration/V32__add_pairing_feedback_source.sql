-- 피드백을 낸 경로를 구분한다(#211). 탈퇴한 회원의 피드백은 member_id만 NULL이 되므로 member_id로는 참여자 익명 피드백과 가를 수 없다.
-- 출처는 낸 시점의 경로라서 회원이 탈퇴해도 MEMBER로 남는다.
-- 기존 NULL 행은 참여자 익명 피드백인지 탈퇴한 회원의 피드백인지 알 수 없어 PARTICIPANT로 채운다.
ALTER TABLE pairing_feedback ADD COLUMN source VARCHAR(255);

UPDATE pairing_feedback
SET source = CASE WHEN member_id IS NULL THEN 'PARTICIPANT' ELSE 'MEMBER' END;

ALTER TABLE pairing_feedback ALTER COLUMN source SET NOT NULL;
ALTER TABLE pairing_feedback ADD CONSTRAINT ck_pairing_feedback_source CHECK (source IN ('MEMBER', 'PARTICIPANT'));
