-- 모임에서 고른 페어링을 id로만 기록한다. 다른 도메인을 가리키는 값이라 FK를 걸지 않는다.
-- 모임마다 하나라서 gathering_id를 기본 키로 두고, 모임장이 바꾸면 이 행을 덮어쓴다.
-- gathering 행에 두지 않는 이유는 참여할 때 모임을 다시 저장하면서 동시에 바꾼 페어링을 옛 값으로 덮어쓰지 않게 하기 위해서다.
CREATE TABLE gathering_pairing (
    gathering_id      BIGINT       NOT NULL,
    drink_id          BIGINT       NOT NULL,
    anju_id           BIGINT       NOT NULL,
    music_mood_id     BIGINT       NOT NULL,
    relationship_type VARCHAR(255) NOT NULL CHECK (relationship_type IN ('BOSS', 'FRIEND', 'COLLEAGUE', 'LOVER')),
    mood_type         VARCHAR(255) NOT NULL CHECK (mood_type IN ('FORMAL', 'CASUAL', 'ROMANTIC', 'CELEBRATORY')),
    CONSTRAINT pk_gathering_pairing PRIMARY KEY (gathering_id),
    CONSTRAINT fk_gathering_pairing_gathering FOREIGN KEY (gathering_id) REFERENCES gathering (id)
);

-- 참여자가 페어링 피드백을 냈다는 표시다. 피드백은 파기하지 않으므로 피드백 쪽이 아니라 참여자 쪽에 두고, 참여자와 함께 파기한다.
-- 참여자마다 한 번만 내므로 participant_id를 기본 키로 두어 동시에 내도 한 행만 생기게 한다.
CREATE TABLE participant_pairing_feedback (
    participant_id BIGINT       NOT NULL,
    submitted_at   TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_participant_pairing_feedback PRIMARY KEY (participant_id),
    CONSTRAINT fk_participant_pairing_feedback_participant FOREIGN KEY (participant_id) REFERENCES participant (id)
);
