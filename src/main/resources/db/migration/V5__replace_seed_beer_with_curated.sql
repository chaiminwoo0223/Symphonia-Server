-- 시드의 '맥주'는 실제 제품이 아니므로 국내 유통 맥주 매핑표(import/curated/beers.csv)의 카스 프레시로 바꾼다.
-- pairing_feedback이 이 행을 FK로 참조하므로 지우지 않고 같은 id를 유지한다.
-- 이 시점에는 BJCP 스타일이 적재되기 전이라 스타일과 맛은 시드 라거 그대로 두고,
-- 이후 적재(./gradlew importDrinks)가 (source, external_id)로 이 행을 찾아 BJCP 스타일과 맛으로 갱신한다.
UPDATE drink
SET name        = '카스 프레시',
    abv         = 4.5,
    source      = 'CURATED',
    external_id = 'cass-fresh'
WHERE source = 'SEED'
  AND external_id = 'beer';
