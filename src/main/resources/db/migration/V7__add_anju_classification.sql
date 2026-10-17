-- 안주에 출처 식별 키와 Harmonize 판정에 쓰는 단일값 속성 3개를 추가한다(#78).
-- 주재료 계열(base), 형태(serving), 매운맛 여부(spicy)만 저장하고, 안주와 Harmonize의 관계는 저장하지 않는다.
-- 기존 행을 채운 뒤에 NOT NULL을 걸어야 행이 있는 DB에서도 컬럼 추가가 실패하지 않는다.
ALTER TABLE anju
    ADD COLUMN source      VARCHAR(255),
    ADD COLUMN external_id VARCHAR(255),
    ADD COLUMN base        VARCHAR(255),
    ADD COLUMN serving     VARCHAR(255),
    ADD COLUMN spicy       BOOLEAN;

-- 시드 6행은 pairing_feedback이 FK로 참조하므로 id를 유지한 채 값만 채운다.
-- 분류 값은 적재 전까지의 초기값이고, 이후 적재(./gradlew importDrinks)가 매핑표 값으로 갱신한다.
UPDATE anju
SET source      = 'SEED',
    external_id = seed.external_id,
    base        = seed.base,
    serving     = seed.serving,
    spicy       = seed.spicy
FROM (VALUES
    (1, 'golbaengi-muchim', 'SEAFOOD', 'MAIN', true),
    (2, 'fried-chicken', 'POULTRY', 'MAIN', false),
    (3, 'tofu-kimchi', 'PORK', 'MAIN', true),
    (4, 'dried-snack', 'SEAFOOD', 'SNACK', false),
    (5, 'cheese-platter', 'HARD_CHEESE', 'APPETIZER', false),
    (6, 'fruit-platter', 'SWEET', 'SNACK', false)
) AS seed (anju_id, external_id, base, serving, spicy)
WHERE anju.id = seed.anju_id;

ALTER TABLE anju
    ALTER COLUMN source SET NOT NULL,
    ALTER COLUMN external_id SET NOT NULL,
    ALTER COLUMN base SET NOT NULL,
    ALTER COLUMN serving SET NOT NULL,
    ALTER COLUMN spicy SET NOT NULL,
    -- 값이 늘어날 때 후속 마이그레이션이 DROP 후 다시 ADD할 수 있도록 check 제약에 이름을 붙인다.
    ADD CONSTRAINT ck_anju_source CHECK (source IN ('SEED', 'K_FIND')),
    ADD CONSTRAINT ck_anju_base CHECK (base IN (
        'POULTRY', 'BEEF', 'PORK', 'LAMB', 'GAME', 'CURED_MEAT', 'SEAFOOD', 'RICH_FISH', 'LEAN_FISH',
        'VEGETABLE', 'NOODLE', 'HARD_CHEESE', 'MATURATED_CHEESE', 'SOFT_CHEESE', 'GOAT_CHEESE', 'SWEET')),
    ADD CONSTRAINT ck_anju_serving CHECK (serving IN ('MAIN', 'APPETIZER', 'SNACK')),
    ADD CONSTRAINT uk_anju_source_external_id UNIQUE (source, external_id);
