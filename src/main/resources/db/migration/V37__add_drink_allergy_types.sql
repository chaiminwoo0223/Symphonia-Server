-- 음료의 알레르기 유발 성분을 담는다(#241). 추천이 참석자 알레르기와 겹치는 음료를 안주와 같게 제외한다.
-- 와인은 wine-type.csv를, 맥주는 스타일별 규칙표(beer-allergy.csv)를 적재 러너가 채운다.
-- 추천은 요청마다 음료 전체를 읽으므로 음식 계열처럼 배열 컬럼에 담는다.
-- 이미 적재된 맥주는 적재를 다시 실행해야 값이 채워진다.
-- 기존 행을 채운 뒤 기본값을 지워, 이후 INSERT는 알레르기를 항상 명시하게 한다.
ALTER TABLE drink
    ADD COLUMN allergy_types VARCHAR(255)[] NOT NULL DEFAULT '{}',
    ADD CONSTRAINT ck_drink_allergy_types CHECK (allergy_types <@ ARRAY[
        'EGG', 'MILK', 'PEANUT', 'WALNUT', 'PINE_NUT', 'SHELLFISH', 'SHRIMP', 'CRAB', 'SQUID', 'MACKEREL',
        'PORK', 'BEEF', 'CHICKEN', 'SOYBEAN', 'WHEAT', 'BUCKWHEAT', 'PEACH', 'TOMATO', 'SULFITE']::VARCHAR(255)[]);

ALTER TABLE drink ALTER COLUMN allergy_types DROP DEFAULT;

-- 적재 러너가 다루지 않는 시드 막걸리는 마이그레이션이 주원료의 알레르기를 넣는다. 막걸리는 밀 누룩을 쓴다.
UPDATE drink
SET allergy_types = ARRAY['WHEAT']::VARCHAR(255)[]
WHERE source = 'SEED'
  AND external_id = 'makgeolli';

-- 와인은 모두 아황산류가 들어간다. 시드 레드 와인을 바꾼 와인(V6)과 이미 적재된 와인이 적재 전에도 걸러지도록 여기서 채운다.
UPDATE drink
SET allergy_types = ARRAY['SULFITE']::VARCHAR(255)[]
WHERE source = 'X_WINES';

-- 시드 두부김치는 적재 전에도 김치의 새우젓이 걸러지도록 넣는다. 적재하면 매핑표와 재료 규칙표로 다시 계산한다.
INSERT INTO anju_allergy_type (anju_id, allergy_type)
SELECT id, 'SHRIMP'
FROM anju
WHERE source = 'SEED'
  AND external_id = 'tofu-kimchi'
ON CONFLICT DO NOTHING;
