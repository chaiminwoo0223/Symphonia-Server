-- 모든 분담금이 끝나고 7일 뒤에 모임을 파기하므로(#128), 분담금 상태를 마지막으로 바꾼 시각을 남긴다.
-- 기록할 때 정해진 상태 그대로인 분담금은 바뀐 시각이 없어 null로 둔다.
ALTER TABLE settlement_share ADD COLUMN status_changed_at TIMESTAMP(6);

-- 이 변경 전에 상태를 바꾼 분담금은 바꾼 시각을 알 수 없다.
-- 분담금 상태를 바꾸면 정산 건을 다시 저장하므로, 그 정산 건이 마지막으로 저장된 시각으로 채운다.
-- 0원 분담금은 확인 완료로 시작해 바뀐 적이 없으므로 채우지 않는다.
UPDATE settlement_share share
SET status_changed_at = settlement.updated_at
FROM settlement
WHERE share.settlement_id = settlement.id
  AND share.status <> 'UNPAID'
  AND share.amount > 0;

-- 파기 스윕이 생성 후 9일이 지난 모임만 후보로 읽는다(#128).
CREATE INDEX idx_gathering_opened_at ON gathering (opened_at);
