-- 음료마다 어울리는 음식 계열(Harmonize)을 담는다(#105). 안주 쪽 판정은 저장하지 않고 Harmonize enum이 규칙으로 정한다(#78).
-- 와인은 X-Wines의 Harmonize 태그를, 맥주는 스타일별 규칙표(beer-harmonize.csv)를 적재 러너가 채운다.
-- 추천은 요청마다 음료 전체를 읽으므로 별도 테이블 대신 배열 컬럼에 담아 음료 행과 함께 한 번에 읽는다.
-- 기존 행을 채운 뒤 기본값을 지워, 이후 INSERT는 음식 계열을 항상 명시하게 한다.
ALTER TABLE drink
    ADD COLUMN harmonizes VARCHAR(255)[] NOT NULL DEFAULT '{}',
    ADD CONSTRAINT ck_drink_harmonizes CHECK (harmonizes <@ ARRAY[
        'POULTRY', 'BEEF', 'LAMB', 'GAME_MEAT', 'PORK', 'SHELLFISH', 'VEAL', 'VEGETARIAN', 'RICH_FISH', 'PASTA',
        'SPICY_FOOD', 'MATURATED_CHEESE', 'HARD_CHEESE', 'CURED_MEAT', 'APPETIZER', 'SNACK', 'SOFT_CHEESE',
        'LEAN_FISH', 'SWEET_DESSERT', 'GOAT_CHEESE']::VARCHAR(255)[]);

ALTER TABLE drink ALTER COLUMN harmonizes DROP DEFAULT;

-- 시드 소주는 적재 러너가 다루지 않아 마이그레이션이 음식 계열을 넣는다.
-- 직장인 회식 설문에서 삼겹살과 소주 조합이 가장 많이 꼽혔고(한화생명 2015, 응답자 65%), 외국인 고객 설문에서도 고기와 소주가 1위였다(강강술래 2014).
-- 근거가 설문으로 확인되는 돼지고기, 소고기만 넣는다.
UPDATE drink
SET harmonizes = ARRAY['PORK', 'BEEF']::VARCHAR(255)[]
WHERE source = 'SEED'
  AND external_id = 'soju';
