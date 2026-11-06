-- 회원은 같은 조합(음료, 안주, 음악)에 피드백을 하나만 남기고, 다시 내면 평점을 바꾼다(#130).
-- 이 규칙 전에 같은 회원이 같은 조합에 여러 번 낸 피드백은 마지막 의사인 id가 가장 큰 행만 남긴다.
DELETE FROM pairing_feedback older
USING pairing_feedback newer
WHERE older.member_id = newer.member_id
  AND older.drink_id = newer.drink_id
  AND older.anju_id = newer.anju_id
  AND older.music_mood_id = newer.music_mood_id
  AND older.id < newer.id;

-- 제출할 때 회원과 조합으로 기존 피드백을 찾으므로 이 제약의 인덱스를 조회에도 쓴다.
-- 익명 피드백(#131)의 member_id NULL은 PostgreSQL이 서로 다른 값으로 보므로 이 제약에 걸리지 않는다.
ALTER TABLE pairing_feedback
    ADD CONSTRAINT uk_pairing_feedback_member_combination UNIQUE (member_id, drink_id, anju_id, music_mood_id);

-- 조합별 평점 집계다. 피드백을 저장하는 트랜잭션 안에서 UPSERT로 갱신한다(#122).
-- 추천은 표본 수로 거른 집계 전체를 읽으므로 조회 조건 인덱스를 두지 않는다.
-- FK 컬럼은 PK의 컬럼이고, 카탈로그를 지울 때는 pairing_feedback이 먼저 막으므로 따로 인덱싱하지 않는다.
CREATE TABLE pairing_rating_summary (
    drink_id      BIGINT  NOT NULL,
    anju_id       BIGINT  NOT NULL,
    music_mood_id BIGINT  NOT NULL,
    like_count    INTEGER NOT NULL CHECK (like_count >= 0),
    dislike_count INTEGER NOT NULL CHECK (dislike_count >= 0),
    CONSTRAINT pk_pairing_rating_summary PRIMARY KEY (drink_id, anju_id, music_mood_id),
    CONSTRAINT fk_pairing_rating_summary_drink FOREIGN KEY (drink_id) REFERENCES drink (id),
    CONSTRAINT fk_pairing_rating_summary_anju FOREIGN KEY (anju_id) REFERENCES anju (id),
    CONSTRAINT fk_pairing_rating_summary_music_mood FOREIGN KEY (music_mood_id) REFERENCES music_mood (id)
);

INSERT INTO pairing_rating_summary (drink_id, anju_id, music_mood_id, like_count, dislike_count)
SELECT drink_id,
       anju_id,
       music_mood_id,
       COUNT(*) FILTER (WHERE rating = 'LIKE'),
       COUNT(*) FILTER (WHERE rating = 'DISLIKE')
FROM pairing_feedback
GROUP BY drink_id, anju_id, music_mood_id;
