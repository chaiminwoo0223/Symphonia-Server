-- 결제(payment)는 결제자의 카드 결제 1건을 가리키므로, 분담의 입금 상태는 분담의 상태로 부른다(#125).
-- 컬럼 이름을 바꿔도 V11에서 자동으로 붙은 CHECK 제약 이름은 그대로라 함께 바꾼다.
ALTER TABLE settlement_share RENAME COLUMN payment_status TO status;
ALTER TABLE settlement_share RENAME CONSTRAINT settlement_share_payment_status_check TO settlement_share_status_check;

-- 0원 분담은 낼 돈이 없어 확인 완료로 시작한다(#125). 이 규칙 전에 저장한 0원 분담은 모두 미입금이라 확인 완료로 맞춘다.
-- #125 전에는 입금 상태를 바꾸는 기능이 없었으므로, 미입금인 0원 분담만 바꾸면 다른 상태의 분담은 건드리지 않는다.
UPDATE settlement_share SET status = 'CONFIRMED' WHERE amount = 0 AND status = 'UNPAID';
