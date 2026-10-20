-- 음료마다 어울리는 음식 계열(Harmonize)을 담는다(#105). 안주 쪽 판정은 저장하지 않고 Harmonize enum이 규칙으로 정한다(#78).
-- 와인은 X-Wines의 Harmonize 태그를, 맥주는 스타일별 규칙표를 적재 러너가 채운다.
CREATE TABLE drink_harmonize (
    drink_id  BIGINT       NOT NULL,
    harmonize VARCHAR(255) NOT NULL,
    CONSTRAINT pk_drink_harmonize PRIMARY KEY (drink_id, harmonize),
    CONSTRAINT fk_drink_harmonize_drink FOREIGN KEY (drink_id) REFERENCES drink (id),
    CONSTRAINT ck_drink_harmonize_harmonize CHECK (harmonize IN (
        'POULTRY', 'BEEF', 'LAMB', 'GAME_MEAT', 'PORK', 'SHELLFISH', 'VEAL', 'VEGETARIAN', 'RICH_FISH', 'PASTA',
        'SPICY_FOOD', 'MATURATED_CHEESE', 'HARD_CHEESE', 'CURED_MEAT', 'APPETIZER', 'SNACK', 'SOFT_CHEESE',
        'LEAN_FISH', 'SWEET_DESSERT', 'GOAT_CHEESE'))
);
