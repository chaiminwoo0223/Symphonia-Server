package com.symphonia.gathering.domain.vo;

import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;

// 모임에서 고른 음료, 안주, 음악과 그 자리의 관계, 분위기다(#131).
// 참여자 피드백에 같은 값을 담으므로 pairing의 구현체가 아니라 id와 enum만 갖는다.
public record GatheringPairing(
        Long drinkId,
        Long anjuId,
        Long musicMoodId,
        RelationshipType relationshipType,
        MoodType moodType) {
    public static GatheringPairing of(
            Long drinkId,
            Long anjuId,
            Long musicMoodId,
            RelationshipType relationshipType,
            MoodType moodType) {
        return new GatheringPairing(drinkId, anjuId, musicMoodId, relationshipType, moodType);
    }
}
