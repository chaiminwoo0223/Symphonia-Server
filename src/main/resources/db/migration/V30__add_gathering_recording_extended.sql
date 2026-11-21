-- 모임장이 기록 기간을 한 번 연장했는지 담는다(#210). 연장하지 않은 기존 모임은 FALSE다.
ALTER TABLE gathering ADD COLUMN recording_extended BOOLEAN NOT NULL DEFAULT FALSE;
