package com.symphonia.gathering.fixture;

import com.symphonia.gathering.application.dto.command.GatheringPairingCommand;
import com.symphonia.gathering.domain.vo.GatheringPairing;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 모임에서 고른 페어링이다. gathering_pairing은 카탈로그 id에 FK를 걸지 않아 저장 테스트에는 고정 id를 그대로 쓴다.
@Getter
@RequiredArgsConstructor
public enum GatheringPairingFixture {
    FRIEND_CASUAL(1L, 2L, 3L, RelationshipType.FRIEND, MoodType.CASUAL),
    LOVER_ROMANTIC(4L, 5L, 6L, RelationshipType.LOVER, MoodType.ROMANTIC);

    private final Long drinkId;
    private final Long anjuId;
    private final Long musicMoodId;
    private final RelationshipType relationshipType;
    private final MoodType moodType;

    public GatheringPairing create() {
        return GatheringPairing.of(drinkId, anjuId, musicMoodId, relationshipType, moodType);
    }

    // 카탈로그 이름을 조회하는 흐름에서는 실제로 저장한 음료, 안주, 음악 무드의 id를 받는다.
    public GatheringPairing createWithReferences(Long drinkId, Long anjuId, Long musicMoodId) {
        return GatheringPairing.of(drinkId, anjuId, musicMoodId, relationshipType, moodType);
    }

    public GatheringPairingCommand createCommand() {
        return new GatheringPairingCommand(
                drinkId, anjuId, musicMoodId, relationshipType, moodType);
    }
}
