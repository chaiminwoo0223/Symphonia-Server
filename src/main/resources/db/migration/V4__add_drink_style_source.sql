-- BJCP 스타일을 적재하면 스타일도 출처별로 식별해야 하므로 drink와 같은 (source, external_id)를 둔다.
ALTER TABLE drink_style
    ADD COLUMN source      VARCHAR(255),
    ADD COLUMN external_id VARCHAR(255);

UPDATE drink_style
SET source      = 'SEED',
    external_id = seed.external_id
FROM (VALUES
    (1, 'soju'),
    (2, 'lager'),
    (3, 'makgeolli'),
    (4, 'red-wine'),
    (5, 'scotch-whisky'),
    (6, 'whisky-highball'),
    (7, 'virgin-mojito'),
    (8, 'non-alcoholic-highball')
) AS seed (drink_style_id, external_id)
WHERE drink_style.id = seed.drink_style_id;

ALTER TABLE drink_style
    ALTER COLUMN source SET NOT NULL,
    ALTER COLUMN external_id SET NOT NULL,
    ADD CONSTRAINT ck_drink_style_source CHECK (source IN ('SEED', 'BJCP')),
    ADD CONSTRAINT uk_drink_style_source_external_id UNIQUE (source, external_id);

-- 국내 유통 맥주처럼 사람이 매핑표로 직접 고른 제품은 CURATED로 구분한다.
ALTER TABLE drink DROP CONSTRAINT ck_drink_source;
ALTER TABLE drink ADD CONSTRAINT ck_drink_source CHECK (source IN ('SEED', 'CURATED'));
