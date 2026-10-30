-- 분담 방식에서 안 마신 분 정액과 술값 분리를 빼면서 술을 마시는지는 받지 않는다(#124).
-- 쓰지 않는 정보를 남겨 두지 않도록 이미 받은 값도 함께 지운다.
ALTER TABLE participant DROP COLUMN non_drinker;
