-- 결제(payment)는 결제자의 카드 결제 1건을 가리키므로, 분담의 입금 상태는 분담의 상태로 부른다(#125).
-- 컬럼 이름을 바꿔도 V11에서 자동으로 붙은 CHECK 제약 이름은 그대로라 함께 바꾼다.
ALTER TABLE settlement_share RENAME COLUMN payment_status TO status;
ALTER TABLE settlement_share RENAME CONSTRAINT settlement_share_payment_status_check TO settlement_share_status_check;
