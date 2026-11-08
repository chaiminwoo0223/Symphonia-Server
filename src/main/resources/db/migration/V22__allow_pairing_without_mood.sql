-- 분위기를 선택 입력으로 바꾼다(#184). 분위기 없이 받은 추천에도 피드백을 남기고 모임 페어링으로 고를 수 있다.
-- 허용 값 CHECK 제약은 그대로 두고, NULL은 CHECK를 통과하므로 따로 바꾸지 않는다.
ALTER TABLE pairing_feedback ALTER COLUMN mood_type DROP NOT NULL;

ALTER TABLE gathering_pairing ALTER COLUMN mood_type DROP NOT NULL;
