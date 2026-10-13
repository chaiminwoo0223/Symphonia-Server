-- X-Wines 와인과 와인 스타일을 적재할 수 있게 출처 check 제약에 X_WINES를 추가한다.
ALTER TABLE drink DROP CONSTRAINT ck_drink_source;
ALTER TABLE drink ADD CONSTRAINT ck_drink_source CHECK (source IN ('SEED', 'CURATED', 'X_WINES'));

ALTER TABLE drink_style DROP CONSTRAINT ck_drink_style_source;
ALTER TABLE drink_style ADD CONSTRAINT ck_drink_style_source CHECK (source IN ('SEED', 'BJCP', 'X_WINES'));

-- 시드의 '레드 와인'은 실제 제품이 아니므로 X-Wines의 Louis Jadot Beaujolais(WineID 111692)로 바꾼다.
-- 시드와 무게감(3), 산미(4)가 같은 Red, Medium-bodied, High Acidity 와인 중 국내에서 구하기 쉽고 도수가 가장 가까운 제품이다.
-- pairing_feedback이 이 행을 FK로 참조하므로 지우지 않고 같은 id를 유지한다.
-- 이 시점에는 와인 스타일이 적재되기 전이라 스타일과 맛은 시드 레드 와인 그대로 두고,
-- 이후 적재(./gradlew importDrinks)가 (source, external_id)로 이 행을 찾아 와인 스타일과 맛으로 갱신한다.
UPDATE drink
SET name        = 'Louis Jadot Beaujolais',
    abv         = 12.5,
    source      = 'X_WINES',
    external_id = '111692'
WHERE source = 'SEED'
  AND external_id = 'red-wine';
