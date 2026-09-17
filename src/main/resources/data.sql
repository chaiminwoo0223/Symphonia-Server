DELETE FROM drink_jpa_entity;
DELETE FROM anju_jpa_entity;
DELETE FROM music_mood_jpa_entity;

INSERT INTO drink_jpa_entity (name, abv, sweetness, bitterness, carbonation, richness) VALUES
    ('소주', 16.5, 2, 2, 0, 1),
    ('맥주', 4.5, 1, 3, 4, 1),
    ('막걸리', 6.0, 3, 1, 2, 2),
    ('레드 와인', 13.0, 2, 2, 0, 3),
    ('위스키', 40.0, 1, 4, 0, 4),
    ('하이볼', 9.0, 2, 2, 4, 1);

INSERT INTO anju_jpa_entity (name, sweetness, bitterness, carbonation, richness) VALUES
    ('골뱅이무침', 3, 1, 0, 2),
    ('후라이드치킨', 2, 0, 0, 5),
    ('두부김치', 1, 2, 0, 2),
    ('마른안주', 1, 1, 0, 1),
    ('치즈플래터', 2, 1, 0, 3),
    ('과일안주', 4, 0, 0, 1);

INSERT INTO music_mood_jpa_entity (title, formality, romance, celebration, comfort, streaming_url) VALUES
    ('잔잔한 재즈', 5, 1, 1, 2, 'https://open.spotify.com/playlist/formal-jazz'),
    ('클래식 피아노', 5, 0, 0, 2, 'https://open.spotify.com/playlist/formal-classical'),
    ('편안한 어쿠스틱', 1, 1, 2, 5, 'https://open.spotify.com/playlist/casual-acoustic'),
    ('레트로 팝', 1, 1, 3, 4, 'https://open.spotify.com/playlist/casual-retro-pop'),
    ('로맨틱 발라드', 2, 5, 1, 3, 'https://open.spotify.com/playlist/romantic-ballad'),
    ('감성 R&B', 2, 5, 2, 3, 'https://open.spotify.com/playlist/romantic-rnb'),
    ('신나는 파티 댄스', 1, 1, 5, 2, 'https://open.spotify.com/playlist/celebratory-dance'),
    ('흥겨운 K-POP', 2, 1, 5, 2, 'https://open.spotify.com/playlist/celebratory-kpop');
