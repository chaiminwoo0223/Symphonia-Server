-- 결제자들이 카카오톡 방을 만들어 정산하므로 결제자 계좌를 더 받지 않는다(#207). 기존 계좌 데이터도 함께 지운다.
DROP TABLE participant_bank_account;
