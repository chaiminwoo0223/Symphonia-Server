-- 정산 메시지 발송을 폐기하며(#193) 알림 테이블과 정산 건의 발송 시각을 지운다.
-- notification_delivery_settlement가 notification_delivery를 참조하므로 참조하는 쪽부터 지운다.
DROP TABLE notification_delivery_settlement;
DROP TABLE notification_delivery;
DROP TABLE notification_schedule;

ALTER TABLE settlement DROP COLUMN notified_at;
