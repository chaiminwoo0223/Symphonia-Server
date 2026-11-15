-- 안주에 맛집 검색어(place_keyword)를 추가한다(#182).
-- 매핑표에서 검색어를 비워 둔 안주는 이름을 검색어로 쓰므로, 기존 행은 이름으로 채운다.
-- 기존 행을 채운 뒤에 NOT NULL을 걸어야 행이 있는 DB에서도 컬럼 추가가 실패하지 않는다.
ALTER TABLE anju
    ADD COLUMN place_keyword VARCHAR(255);

UPDATE anju
SET place_keyword = name;

ALTER TABLE anju
    ALTER COLUMN place_keyword SET NOT NULL;
