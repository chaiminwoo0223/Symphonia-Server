-- 프로필 이미지 URL은 소셜 제공자가 정해 길이를 알 수 없고, 255자를 넘을 수 있다(#245).
-- 넘치면 가입이 매번 실패하므로 길이 제한이 없는 text로 넓힌다.
ALTER TABLE member ALTER COLUMN profile_image TYPE TEXT;
