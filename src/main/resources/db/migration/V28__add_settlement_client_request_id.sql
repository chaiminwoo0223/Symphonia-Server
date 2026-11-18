-- 정산 건 기록 요청을 같은 요청 id로 다시 보내도 한 건만 만들도록 요청 id를 둔다(#174). 기존 정산 건에는 요청 id가 없어 NULL로 둔다.
ALTER TABLE settlement ADD COLUMN client_request_id UUID;

-- 결제자마다 요청 id를 한 번만 쓰게 한다. NULL은 서로 같지 않아 기존 정산 건끼리 충돌하지 않는다.
ALTER TABLE settlement ADD CONSTRAINT uk_settlement_payer_client_request UNIQUE (payer_participant_id, client_request_id);

-- 이 제약의 선두 컬럼이 payer_participant_id라서 결제자별 조회를 대신한다. 같은 컬럼의 기존 인덱스는 쓰기 비용만 늘리므로 지운다.
DROP INDEX idx_settlement_payer_participant_id;
