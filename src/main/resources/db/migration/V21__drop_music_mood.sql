-- 추천에서 음악을 빼고 조합을 음료×안주로 줄인다(#184).
-- 같은 회원이 같은 음료×안주에 음악만 다르게 낸 피드백은 마지막으로 제출한 행만 남긴다.
-- 재제출은 기존 행을 고치므로 id가 아니라 updated_at으로 마지막 의사를 가리고, 같으면 id가 큰 행을 남긴다.
-- 익명 피드백은 member_id가 NULL이라 같은 사람인지 알 수 없으므로 모두 남긴다.
DELETE FROM pairing_feedback
WHERE id IN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY member_id, drink_id, anju_id
                   ORDER BY updated_at DESC NULLS LAST, id DESC
               ) AS recency
        FROM pairing_feedback
        WHERE member_id IS NOT NULL
    ) ranked
    WHERE recency > 1
);

ALTER TABLE pairing_feedback
    DROP CONSTRAINT uk_pairing_feedback_member_combination,
    DROP CONSTRAINT fk_pairing_feedback_music_mood,
    DROP COLUMN music_mood_id,
    ADD CONSTRAINT uk_pairing_feedback_member_combination UNIQUE (member_id, drink_id, anju_id);

-- 음악별 집계를 더하면 지운 중복 피드백까지 세므로, 남은 피드백으로 다시 집계한다.
DELETE FROM pairing_rating_summary;

ALTER TABLE pairing_rating_summary
    DROP CONSTRAINT pk_pairing_rating_summary,
    DROP CONSTRAINT fk_pairing_rating_summary_music_mood,
    DROP COLUMN music_mood_id,
    ADD CONSTRAINT pk_pairing_rating_summary PRIMARY KEY (drink_id, anju_id);

INSERT INTO pairing_rating_summary (drink_id, anju_id, like_count, dislike_count)
SELECT drink_id,
       anju_id,
       COUNT(*) FILTER (WHERE rating = 'LIKE'),
       COUNT(*) FILTER (WHERE rating = 'DISLIKE')
FROM pairing_feedback
GROUP BY drink_id, anju_id;

ALTER TABLE gathering_pairing DROP COLUMN music_mood_id;

DROP TABLE music_mood;
