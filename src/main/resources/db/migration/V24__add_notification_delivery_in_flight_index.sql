-- 폴러가 주기마다 발행했지만 아직 보내지 못한 발송 기록 수를 세므로(#173), 그 행만 인덱스에 둔다.
-- 리마인드는 며칠 뒤에 보낼 PENDING 행이 쌓이므로, PENDING 전체를 훑지 않도록 발행한 행으로 좁힌다.
CREATE INDEX idx_notification_delivery_in_flight ON notification_delivery (published_at)
    WHERE status = 'PENDING' AND published_at IS NOT NULL;
