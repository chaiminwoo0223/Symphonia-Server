-- 미입금 리마인드(#127)를 발송 기록으로 두므로 발송 종류에 REMINDER를 더한다.
ALTER TABLE notification_delivery DROP CONSTRAINT notification_delivery_type_check;
ALTER TABLE notification_delivery ADD CONSTRAINT notification_delivery_type_check
    CHECK (type IN ('SETTLEMENT', 'CANCELLATION', 'REMINDER'));

-- 리마인드는 보내기 직전에 모임의 미입금 분담금을 모두 담는다(#127).
-- 그래서 notification_delivery_settlement는 만들 때 넣은 뒤 바꾸지 않는다는 V14의 설명과 달리, 컨슈머가 맡을 때마다 실제로 담은 정산 건으로 바뀐다.
-- 리마인드를 만들 때 같은 모임의 리마인드를 gathering_id로 찾으므로 idx_notification_delivery_gathering_id를 그대로 쓴다.
