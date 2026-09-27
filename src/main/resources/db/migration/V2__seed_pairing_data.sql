INSERT INTO drink (id, name, abv, sweetness, bitterness, carbonation, richness, acidity, non_alcoholic) VALUES
    (1, '소주', 16.5, 2, 2, 0, 1, 1, false),
    (2, '맥주', 4.5, 1, 3, 4, 1, 2, false),
    (3, '막걸리', 6.0, 3, 1, 2, 2, 3, false),
    (4, '레드 와인', 13.0, 2, 2, 0, 3, 4, false),
    (5, '위스키', 40.0, 1, 4, 0, 4, 1, false),
    (6, '하이볼', 9.0, 2, 2, 4, 1, 2, false),
    (7, '버진 모히또', 0.0, 3, 1, 4, 1, 4, true),
    (8, '논알콜 하이볼', 0.0, 2, 2, 4, 1, 2, true);

INSERT INTO anju (id, name, sweetness, bitterness, carbonation, richness, acidity) VALUES
    (1, '골뱅이무침', 3, 1, 0, 2, 4),
    (2, '후라이드치킨', 2, 0, 0, 5, 0),
    (3, '두부김치', 1, 2, 0, 2, 2),
    (4, '마른안주', 1, 1, 0, 1, 0),
    (5, '치즈플래터', 2, 1, 0, 3, 1),
    (6, '과일안주', 4, 0, 0, 1, 3);

INSERT INTO anju_allergy_type (anju_id, allergy_type) VALUES
    (1, 'WHEAT'),
    (2, 'WHEAT'),
    (2, 'EGG'),
    (2, 'CHICKEN'),
    (3, 'SOYBEAN'),
    (3, 'PORK'),
    (4, 'SQUID'),
    (4, 'PEANUT'),
    (5, 'MILK');

INSERT INTO music_mood (id, title, formality, romance, celebration, comfort, streaming_url) VALUES
    (1, '잔잔한 재즈', 5, 1, 1, 2, 'https://open.spotify.com/playlist/formal-jazz'),
    (2, '클래식 피아노', 5, 0, 0, 2, 'https://open.spotify.com/playlist/formal-classical'),
    (3, '편안한 어쿠스틱', 1, 1, 2, 5, 'https://open.spotify.com/playlist/casual-acoustic'),
    (4, '레트로 팝', 1, 1, 3, 4, 'https://open.spotify.com/playlist/casual-retro-pop'),
    (5, '로맨틱 발라드', 2, 5, 1, 3, 'https://open.spotify.com/playlist/romantic-ballad'),
    (6, '감성 R&B', 2, 5, 2, 3, 'https://open.spotify.com/playlist/romantic-rnb'),
    (7, '신나는 파티 댄스', 1, 1, 5, 2, 'https://open.spotify.com/playlist/celebratory-dance'),
    (8, '흥겨운 K-POP', 2, 1, 5, 2, 'https://open.spotify.com/playlist/celebratory-kpop');

-- id를 명시해 넣으면 IDENTITY 시퀀스가 전진하지 않으므로, 다음 INSERT가 시드 id와 충돌하지 않게 최댓값으로 맞춘다.
SELECT setval(pg_get_serial_sequence('drink', 'id'), (SELECT MAX(id) FROM drink));
SELECT setval(pg_get_serial_sequence('anju', 'id'), (SELECT MAX(id) FROM anju));
SELECT setval(pg_get_serial_sequence('music_mood', 'id'), (SELECT MAX(id) FROM music_mood));
