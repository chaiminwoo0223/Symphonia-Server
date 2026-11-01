-- 모임의 정산 메시지 발송 예약이다(#126). 모임마다 한 행이고, 처음 정산 건을 기록할 때 만든다.
-- 보낼 정산 건이 없으면 scheduled_at을 비운다. 기록과 발송이 같은 행을 잠가 예약 시각을 차례로 바꾼다.
CREATE TABLE notification_schedule (
    gathering_id BIGINT NOT NULL,
    scheduled_at TIMESTAMP(6),
    CONSTRAINT pk_notification_schedule PRIMARY KEY (gathering_id),
    CONSTRAINT fk_notification_schedule_gathering FOREIGN KEY (gathering_id) REFERENCES gathering (id)
);

-- 폴러가 짧은 주기로 예약 시각이 지난 예약을 찾으므로, 예약이 있는 행만 인덱스에 둔다.
CREATE INDEX idx_notification_schedule_scheduled_at ON notification_schedule (scheduled_at)
    WHERE scheduled_at IS NOT NULL;
