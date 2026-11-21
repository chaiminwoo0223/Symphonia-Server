-- 모임 열기 요청을 같은 요청 id로 다시 보내도 모임이 한 개만 만들어지도록 요청 id를 둔다(#217). 기존 모임에는 요청 id가 없어 NULL로 둔다.
ALTER TABLE gathering ADD COLUMN client_request_id UUID;

-- 모임장마다 요청 id를 한 번만 쓰게 한다. NULL은 서로 같지 않아 기존 모임끼리 충돌하지 않는다.
ALTER TABLE gathering ADD CONSTRAINT uk_gathering_host_client_request UNIQUE (host_member_id, client_request_id);

-- 이 제약의 선두 컬럼이 host_member_id라서 모임장별 조회를 대신한다. 같은 컬럼의 기존 인덱스는 쓰기 비용만 늘리므로 지운다.
DROP INDEX idx_gathering_host_member_id;
