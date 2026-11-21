-- 모임장이 탈퇴하기 전에 진행 중인 모임을 알려 주려고 모임장 회원 id로 모임을 읽는다(#210).
CREATE INDEX idx_gathering_host_member_id ON gathering (host_member_id);
