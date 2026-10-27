-- V2 시드에 기대지 않고 빈 카탈로그에서 시작하기 위해 삭제한다. 테스트 트랜잭션과 함께 롤백된다.
DELETE FROM pairing_feedback;
DELETE FROM anju_allergy_type;
DELETE FROM drink;
DELETE FROM drink_style;
DELETE FROM anju;
DELETE FROM music_mood;
